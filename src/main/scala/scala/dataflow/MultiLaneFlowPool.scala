package scala.dataflow

import scala.annotation.tailrec
import jsr166y._

class MultiLaneFlowPool[T](val lanes: Int) extends FlowPool[T] {

  import FlowPool._
  import SingleLaneFlowPool._
  
  val initBlocks = Array.fill(lanes)(newBlock(0))
  
  def newPool[S] = new MultiLaneFlowPool[S](lanes)

  def builder = new MultiLaneBuilder[T](initBlocks)

  def doForAll[U](f: T => U): Future[Int] = {
    val fut = new Future[Int]()

    // TODO find way to sync CB completion
    // Have a thing that, when called N times, fetches Seal count and ends
    for (b <- initBlocks) {
      val cbe = new CallbackElem(f, fut.complete _, CallbackNil, initBlocks(0), 0)
      task(new RegisterCallbackTask(cbe))
    }

    fut
  }

  // TODO the following is uttermost bullshit when having multiple lanes!!!
  def mappedFold[U, V <: U](accInit: V)(cmb: (U,V) => V)(map: T => U): Future[(Int, V)] = {
    /* We do not need to synchronize on this var, because IN THE
     * CURRENT SETTING, callbacks are only executed in sequence.
     * This WILL break if the scheduling changes
     */
    @volatile var acc = accInit

    doForAll { x =>
      acc = cmb(map(x), acc)
    } map {
      c => (c,acc)
    }
  }

}



object MultiLaneFlowPool {
  import FlowPool._

  final class RegisterCallbackTask[T](val cb: CallbackElem[T]) extends RecursiveAction {
    private val unsafe = getUnsafe()
    private val ARRAYOFFSET      = unsafe.arrayBaseOffset(classOf[Array[AnyRef]])
    private val ARRAYSTEP        = unsafe.arrayIndexScale(classOf[Array[AnyRef]])
    @inline private def RAWPOS(idx: Int) = ARRAYOFFSET + idx * ARRAYSTEP
    @inline private def CAS(bl: Array[AnyRef], idx: Int, ov: AnyRef, nv: AnyRef) =
      unsafe.compareAndSwapObject(bl, RAWPOS(idx), ov, nv)

    @tailrec
    def compute() {
      val curo = /*READ*/cb.block(cb.pos)
      curo match {
        // At (sealed) end of buffer
        case Seal(sz, null) => 
          cb.endf(sz)
        // At end of current elements
        case cbh: CallbackHolder[T] => {
          val newel = cbh.insertedCallback(cb)
          if (!CAS(cb.block, cb.pos, curo, newel)) compute()
        }
        // Some element
        case v => {
          cb.func(v.asInstanceOf[T])
          cb.pos = cb.pos + 1
          if (cb.pos >= LAST_CALLBACK_POS) endOfBlock()
          else compute()
        }
      }
    }

    private def endOfBlock() {
      val curcb = cb.block(LAST_CALLBACK_POS)

      // Check if last callback is seal for early stopping
      curcb match {
        case Seal(sz, null) => {
          cb.endf(sz)
          return
        }
        case _ => 
      }

      // We need to move on
      val mexp = cb.block(MUST_EXPAND_POS)
      mexp match {
        case Next(b) => {
          // We can safely set here as nobody knows about the CBElem yet
          cb.block = b
          cb.pos = 0
        }
        case me @ MustExpand => {
          val curidx = cb.block(IDX_POS).asInstanceOf[Int]
          val curblock = cb.block

          // Insert CB in List
          val newel = curcb.asInstanceOf[CallbackHolder[_]].insertedCallback(cb)

          // prepare next block
          val nextblock = newBlock(curidx+1,newel)

          // prepare callback to be added
          cb.block = nextblock
          cb.pos = 0
    
          // Swap block in an end.
          if (CAS(curblock, MUST_EXPAND_POS, me, Next(nextblock))) return

          // We failed CASing. We have another Next now. Update and move on
          cb.block = curblock(MUST_EXPAND_POS).asInstanceOf[Next].block
          cb.pos = 0
          
        }
        case _ => sys.error("SingleLaneFlowPool block in inconsistent state: " +
                            "Unknown object at MUST_EXPAND_POS. Epic " +
                            "Fail you DIE (miserably).") 
      }

      task(new RegisterCallbackTask(cb))
      
    }

  }
  
}
