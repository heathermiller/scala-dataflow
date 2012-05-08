package scala.dataflow.impl

import scala.annotation.tailrec
import scala.dataflow.FlowPoolLike

class FlowPool[T <: AnyRef] extends FlowPoolLike[T] {

  import FlowPool._
  
  private val initBlock = new Array[AnyRef](blockSize + 2)

  initBlock(0) = CBNil
  
  override def builder: Builder[T] =
    new Builder[T](initBlock)

  override def foreach[U](f: T => U) {}
  /*
  override def foreach[U](f: T => U) {
    // TODO it is useless to allocate an object here. What to do?
    // I hope this is optimized an put on stack...
    // probably not, but doesn't really matter - there aren't that many callbacks
    // ! real problem is - CBWrite captures the this reference to the FlowPool
    val w = new CBWriter(initBlock)
    w.addCB(f)
  }
  
  private class CBWriter(bl: Array[AnyRef]) extends BlockFinder(bl) {
    
    @tailrec
    private def findNonFull(cb: T => Any): AnyRef = {
      var cobj = b(i)
      if (!isElem(cobj)) cobj
      else {
        cb(CAST[T](cobj))
        advance();
        findNonFull(cb)
      }
    }
    
    def addCB(cb: T => Any) {
      var cur_obj: AnyRef = null 
      var new_obj: AnyRef = null
      do {
        cur_obj = findNonFull(cb)
        if (cur_obj eq FlowPool.Seal) return
        
        new_obj =
          new CBElem(cb, CAST[CBElem[T]](cur_obj))

      } while(!CAS(b, i, cur_obj, new_obj));
    }
  }
  * */

}

object FlowPool {

  private val blockSize = 2000
  
  final class Builder[T <: AnyRef](bl: Array[AnyRef]) extends FlowPoolLike.Builder[T] {

    @volatile private var lastpos = 0
    @volatile private var block   = bl

    private val unsafe = getUnsafe()
    private val blockSize = 2000
    private val ARRAYOFFSET = unsafe.arrayBaseOffset(classOf[Array[AnyRef]])
    private val ARRAYSTEP   = unsafe.arrayIndexScale(classOf[Array[AnyRef]])
    @inline private def RAWPOS(idx: Int) = ARRAYOFFSET + idx * ARRAYSTEP 
    @inline private def CAS(idx: Int, exp: Any, x: Any) =
      unsafe.compareAndSwapObject(block, RAWPOS(idx), exp, x)
    
    @tailrec
    def <<(x: T) = 
      if (lastpos < blockSize) { 
        val pos = lastpos
        val npos = pos + 1
        val next = block(npos)
        val curo = block(pos)
        if (curo.isInstanceOf[CBList[T]]) {
          if (CAS(npos, next, curo)) {
            if (CAS(pos, curo, x)) {
              lastpos = npos
              applyCBs(curo.asInstanceOf[CBList[T]], x)
              this
            } else <<(x)
          } else <<(x)
        } else {
          advance()
          <<(x)
        }
      } else {
        advance()
        <<(x)
      }

    def seal() {
      // TODO
    }

    @tailrec
    private def advance() {
      val pos = lastpos
      val obj = block(pos)
      if (obj eq Seal) sys.error("Insert on sealed structure")
      if (!obj.isInstanceOf[CBList[_]]) {
        lastpos = pos + 1
        advance()
      } else if (pos >= blockSize) {
        val ob = block(blockSize + 1).asInstanceOf[Array[AnyRef]]
        if (ob eq null) {
          val nb = new Array[AnyRef](blockSize + 2)
          nb(0) = block(blockSize)
          CAS(blockSize + 1, ob, nb)
        }
        // TODO we have a race here
        block = block(blockSize + 1).asInstanceOf[Array[AnyRef]]
        lastpos = 0
      }
    }

    /*
    private def advance() {
      var pos = lastpos
      while (!block(pos).isInstanceOf[CBList[_]]) {
        if (block(pos) eq Seal) sys.error("Insert on sealed structure")
        pos += 1
      }
      if (pos < blockSize) {
        lastpos = pos
      } else {
        val ob = block(blockSize + 1).asInstanceOf[Array[AnyRef]]
        if (ob eq null) {
          val nb = new Array[AnyRef](blockSize + 2)
          nb(0) = block(blockSize)
          CAS(blockSize + 1, ob, nb)
        }
        // TODO race here
        block = block(blockSize + 1).asInstanceOf[Array[AnyRef]]
        lastpos = 0
      }
    }
    */

    @tailrec
    private def applyCBs[T](e: CBList[T], obj: T): Unit = e match {
      case el: CBElem[T] => el.elem(obj); applyCBs(el.next, obj)
      case _ =>
    }

  }

  private sealed class CBList[-T]
  private final class CBElem[-T] (
    val elem: T => Any,
    val next: CBElem[T]
  ) extends CBList[T]
  private final object CBNil extends CBList[Any]

  private object Seal;

}
