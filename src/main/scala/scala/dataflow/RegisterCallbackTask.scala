package scala.dataflow

import scala.annotation.tailrec
import jsr166y._

final class RegisterCallbackTask[T, S](val cb: CallbackElem[T, S]) extends RecursiveAction {
  import FlowPool._

  private val unsafe = getUnsafe()
  private val ARRAYOFFSET      = unsafe.arrayBaseOffset(classOf[Array[AnyRef]])
  private val ARRAYSTEP        = unsafe.arrayIndexScale(classOf[Array[AnyRef]])
  @inline private def RAWPOS(idx: Int) = ARRAYOFFSET + idx * ARRAYSTEP
  @inline private def CAS(bl: Array[AnyRef], idx: Int, ov: AnyRef, nv: AnyRef) =
    unsafe.compareAndSwapObject(bl, RAWPOS(idx), ov, nv)

  @tailrec
  def compute() {
    val curo = /*READ*/cb.block(cb.position)
    curo match {
      // At (sealed) end of buffer
      case Seal(sz, null) => 
        cb.finalizer(sz, cb.accumulator)
      // At end of current elements
      case cbh: CallbackHolder[T] => {
        val newel = cbh.insertedCallback(cb)
        if (!CAS(cb.block, cb.position, curo, newel)) compute()
      }
      // Some element
      case v => {
        cb.accumulator = cb.folder(cb.accumulator, v.asInstanceOf[T])
        cb.position = cb.position + 1
        if (cb.position >= LAST_CALLBACK_POS) endOfBlock()
        else compute()
      }
    }
  }

  private def endOfBlock() {
    val curcb = cb.block(LAST_CALLBACK_POS)

    // Check if last callback is seal for early stopping
    curcb match {
      case Seal(sz, null) => {
        cb.finalizer(sz, cb.accumulator)
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
        cb.position = 0
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
        cb.position = 0
        
        // Swap block in an end.
        if (CAS(curblock, MUST_EXPAND_POS, me, Next(nextblock))) return

        // We failed CASing. We have another Next now. Update and move on
        cb.block = curblock(MUST_EXPAND_POS).asInstanceOf[Next].block
        cb.position = 0
        
      }
      case _ => sys.error("SingleLaneFlowPool block in inconsistent state: " +
                          "Unknown object at MUST_EXPAND_POS. Epic " +
                          "Fail you DIE (miserably).") 
    }

    task(new RegisterCallbackTask(cb))
    
  }

}
