package scala.dataflow

import scala.annotation.tailrec
import jsr166y._

final class SingleLaneBuilder[T](bl: Array[AnyRef]) extends Builder[T] {
  @volatile private var position = Next(bl)
  
  private val unsafe = getUnsafe()
  private val ARRAYOFFSET      = unsafe.arrayBaseOffset(classOf[Array[AnyRef]])
  private val ARRAYSTEP        = unsafe.arrayIndexScale(classOf[Array[AnyRef]])
  private val BLOCKFIELDOFFSET = unsafe.objectFieldOffset(classOf[SingleLaneBuilder[_]].getDeclaredField("position"))
  @inline private def RAWPOS(idx: Int) = ARRAYOFFSET + idx * ARRAYSTEP
  @inline private def CAS(bl: Array[AnyRef], idx: Int, ov: AnyRef, nv: AnyRef) =
    unsafe.compareAndSwapObject(bl, RAWPOS(idx), ov, nv)
  def CAS_BLOCK_PTR(ov: Next, nv: Next) =
    unsafe.compareAndSwapObject(this, BLOCKFIELDOFFSET, ov, nv)
  
  @tailrec
  def <<(x: T): this.type = {
    val p = /*READ*/position
    val curblock = p.block
    val pos = /*READ*/p.index
    val npos = pos + 1
    val next = curblock(npos)
    val curo = curblock(pos)

    if (curo.isInstanceOf[CallbackList[_]] && ((next eq null) || next.isInstanceOf[CallbackList[_]])) {
      if (CAS(curblock, npos, next, curo)) {
        if (CAS(curblock, pos, curo, x.asInstanceOf[AnyRef])) {
          p.index = npos
          applyCallbacks(curo.asInstanceOf[CallbackList[T]])
          this
        } else <<(x)
      } else <<(x)
    } else {
      if (tryAdd(x)) this
      else <<(x)
    }
  }
  
  def seal(size: Int) {
    val p = /*READ*/position
    val curblock = p.block
    val pos = /*READ*/p.index
    seal(size, curblock, pos)
  }
  
  @tailrec
  private def seal(size: Int, curblock: Array[AnyRef], pos: Int) {
    import FlowPool._

    curblock(pos) match {
      case MustExpand =>
        expand(curblock)
      case Next(block) =>
        seal(size, block, 0)
      case cbl: CallbackList[_] =>
        if (pos < LAST_CALLBACK_POS) {
          val total = totalElems(curblock, pos)
          val nseal =
            if (total < size) Seal(size, cbl)
            else if (total == size) Seal(size, null)
            else sys.error("sealing with %d < number of elements in flow-pool %d".format(size, total))
          if (CAS(curblock, pos, cbl, nseal)) {
            applyCallbacks(cbl)
          } else seal(size, curblock, pos)
        } else seal(size, curblock, pos + 1)
      case Seal(sz, _) =>
        if (size != sz) sys.error("already sealed at %d (!= %d)".format(sz, size))
      case _ =>
        seal(size, curblock, pos + 1)
    }
  }
  
  private def totalElems(curblock: Array[AnyRef], pos: Int) = {
    import FlowPool._
    val blockidx = curblock(IDX_POS).asInstanceOf[Int]
    blockidx * MAX_BLOCK_ELEMS + pos
  }
  
  private def goToNext(next: Next) {
    //CAS_BLOCK_PTR(oldposition, next)
    position = next // ok - not racey
  }
  
  private def expand(curblock: Array[AnyRef]) {
    import FlowPool._

    val at = MUST_EXPAND_POS
    val curidx = curblock(IDX_POS).asInstanceOf[Int]

    // Prepare new block with CBs
    val nextblock = newBlock(curidx + 1,curblock(at - 1))
    
    val next = Next(nextblock)
    if (CAS(curblock, at, MustExpand, next)) {
      // take a shortcut here
      goToNext(next)
    }
  }
  
  
  private def tryAdd(x: T): Boolean = {
    import FlowPool._
    
    val p = /*READ*/position
    val curblock = p.block
    val pos = /*READ*/p.index
    val obj = curblock(pos)

    obj match {
      case Seal(sz, null) => // flowpool sealed here - error
        sys.error("Insert on a sealed structure.")
      case MustExpand => // must extend with a new block
        expand(curblock)
      case ne @ Next(_) => // the next block already exists - go to it
        goToNext(ne)
      case cbh: CallbackHolder[_] => // a list of callbacks here - check if this is the end of the block
        val nextelem = curblock(pos + 1)
        nextelem match {
          case MustExpand =>
            expand(curblock)
          case ne @ Next(_) =>
            goToNext(ne)
          case _: CallbackHolder[_] | null =>
            // current is Seal(sz, _ != null), next is not at the end
            // check size and append
            val curelem = curblock(pos)
            curelem match {
              case Seal(sz, cbs) =>
                val total = totalElems(curblock, pos)
                val nseal = if (total < (sz - 1)) curelem else Seal(sz, null)
                if (CAS(curblock, pos + 1, nextelem, nseal)) {
                  if (CAS(curblock, pos, curelem, null)) {
                    p.index = pos + 1
                    applyCallbacks(cbh.callbacks)
                    return true
                  }
                }
              case _ =>
            }
          case _ =>
        }
      case _ => // a regular object - advance
        throw new Exception
        p.index = pos + 1
        tryAdd(x)
    }
    false
  }
  
  @tailrec
  private def applyCallbacks[T](e: CallbackList[T]): Unit = e match {
    case el: CallbackElem[T] =>
      el.awakeCallback()
      applyCallbacks(el.next)
    case _ =>
  }
  
}
