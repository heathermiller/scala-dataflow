package scala.dataflow.impl

import scala.annotation.tailrec
import scala.dataflow.FlowPoolLike

class FlowPool[T <: AnyRef] extends FlowPoolLike[T] {

  import FlowPool._
  
  private val initBlock = createBlock
  
  override def builder: Builder[T] =
    new Builder[T](initBlock)

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

}

object FlowPool {

  private val unsafe = getUnsafe()
  private val blockSize = 200
  
  private val ARRAYOFFSET = unsafe.arrayBaseOffset(classOf[Array[AnyRef]])
  private val ARRAYSTEP   = unsafe.arrayIndexScale(classOf[Array[AnyRef]])
  private def RAWPOS(idx: Int) = ARRAYOFFSET + idx * ARRAYSTEP 
  private def CAS(trg: Any, idx: Int, exp: Any, x: Any) =
    unsafe.compareAndSwapObject(trg, RAWPOS(idx), exp, x)
  private def CAST[T](v: AnyRef) = v.asInstanceOf[T]

  class BlockFinder(bl: Array[AnyRef]) {
    @volatile
    var b = bl
    var i = 0
    
    final protected def nextBlock() = {
      val next = b(blockSize - 1)
        
      if (next eq null) {
        val nb = createBlock
        CAS(b, blockSize - 1, next, nb)
      }
      
      CAST[Array[AnyRef]](b(blockSize - 1))
    }
    
    final protected def advance() {
      if (i >= blockSize - 2) {
        b = nextBlock() 
        i = 0
      } else {
        i = i + 1
      }
    }
    
    @tailrec
    final protected def findNonFull(): AnyRef = {
      var cobj = b(i)
      if (!isElem(cobj)) cobj
      else {
        advance();
        findNonFull()
      }
    }

  }
  
  // TODO make this builder threadsafe
  class Builder[T <: AnyRef](bl: Array[AnyRef])
  	extends BlockFinder(bl) with FlowPoolLike.Builder[T] {

    @volatile
    var nextb: Array[AnyRef] = null
    var nexti: Int = 0
    
    def <<(x: T) = { write(x); this }
    def seal() { write(Seal) }
        
        
    private def peek() {
      if (i >= blockSize - 2) {
        nextb = nextBlock()
        nexti = 0
      } else {
        nextb = b
        nexti = i
      }
    }
    
    private def write(obj: AnyRef) = {
      var cur_obj:  AnyRef = null
      var next_obj: AnyRef = null
      
      do {
        cur_obj = findNonFull()
        
        if (cur_obj eq Seal)
          sys.error("Attempted write to sealed buffer")

        peek()
        
        next_obj = nextb(nexti)
        cur_obj  = b(i)

      } while (isElem(cur_obj) ||
               !CAS(nextb, nexti, next_obj, cur_obj) ||
    		   !CAS(b, i, cur_obj, obj));
      
      applyCBs(CAST[CBElem[T]](cur_obj), obj)

    }
    
  }
  
  @tailrec
  private def applyCBs[T](e: CBElem[T], obj: AnyRef) {
    if (e eq null) return
    e.elem(CAST[T](obj))
    applyCBs(e.next, obj)
  }

  private def isElem(obj: AnyRef) =
    (obj ne null) && (obj ne Seal) &&
    (obj.getClass() ne classOf[CBElem[_]])

  private def createBlock = new Array[AnyRef](blockSize)

  private final class CBElem[-T] (
    val elem: T => Any,
    val next: CBElem[T] // null here means end
  )

  private object Seal;

}
