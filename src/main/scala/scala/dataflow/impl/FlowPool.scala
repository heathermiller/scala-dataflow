package scala.dataflow.impl

import scala.annotation.tailrec
import scala.dataflow.FlowPoolLike

class FlowPool[T <: AnyRef] {

  import FlowPool._
  
  private def BLOCKSIZE = 512
  
  val initBlock = FlowPool.newBlock
  
  def builder: Builder[T] = new Builder[T](initBlock)

  def foreach[U](f: T => U) {
    (new CBWriter(initBlock)).addCB(f)
  }

}

object FlowPool {

  private val BLOCKSIZE = 512
  
  def newBlock = {
    val bl = new Array[AnyRef](BLOCKSIZE + 4)
    bl(0) = CBNil
    bl(BLOCKSIZE) = End
    bl
  }
  
  private final class CBWriter[T](bl: Array[AnyRef]) {
    
    @volatile private var lastpos = 0
    @volatile private var block   = bl
    private val unsafe = getUnsafe()
    private val ARRAYOFFSET = unsafe.arrayBaseOffset(classOf[Array[AnyRef]])
    private val ARRAYSTEP   = unsafe.arrayIndexScale(classOf[Array[AnyRef]])
    @inline private def RAWPOS(idx: Int) = ARRAYOFFSET + idx * ARRAYSTEP 
    @inline private def CAS(idx: Int, exp: Any, x: Any) =
      unsafe.compareAndSwapObject(block, RAWPOS(idx), exp, x)

    @tailrec
    private def advance(cb: T => Any) {
      val pos = lastpos
      val obj = block(pos)
      if (obj eq Seal) return
      if (!obj.isInstanceOf[CBList[_]]) {
        cb(obj.asInstanceOf[T])
        lastpos = pos + 1
        advance(cb)
      } else if (pos >= BLOCKSIZE) {
        val ob = block(BLOCKSIZE + 1).asInstanceOf[Array[AnyRef]]
        if (ob eq null) {
          val nb = new Array[AnyRef](BLOCKSIZE + 2)
          nb(0) = block(BLOCKSIZE)
          CAS(BLOCKSIZE + 1, ob, nb)
        }
        // TODO we have a race here
        block = block(BLOCKSIZE + 1).asInstanceOf[Array[AnyRef]]
        lastpos = 0
      }
    }
    
    @tailrec
    def addCB(cb: T => Any) {
      if (lastpos < BLOCKSIZE) {
        val pos = lastpos
        val curo = block(pos)
        if (curo.isInstanceOf[CBList[T]]) {
          val no = new CBElem(cb, curo.asInstanceOf[CBList[T]])
          if (!CAS(pos, curo, no)) addCB(cb)
        } else {
          advance(cb)
          addCB(cb)
        }
      } else {
        advance(cb)
        addCB(cb)
      }
    }
  }

  
}


final class Builder[T <: AnyRef](bl: Array[AnyRef]) extends FlowPoolLike.Builder[T] {

  @volatile private var lastpos = 0
  @volatile private var block   = bl

  private val unsafe = getUnsafe()
  private val ARRAYOFFSET = unsafe.arrayBaseOffset(classOf[Array[AnyRef]])
  private val ARRAYSTEP   = unsafe.arrayIndexScale(classOf[Array[AnyRef]])
  @inline private def RAWPOS(idx: Int) = ARRAYOFFSET + idx * ARRAYSTEP
  @inline private def CAS(idx: Int, exp: Any, x: Any) =
    unsafe.compareAndSwapObject(block, RAWPOS(idx), exp, x)
  
  private def BLOCKSIZE = 512
  
  @tailrec
  def <<(x: T) = {
    val pos = lastpos
    val npos = pos + 1
    val next = block(npos)
    val curo = block(pos)
    if (curo.isInstanceOf[CBList[T]] && ((next eq null) || next.isInstanceOf[CBList[_]])) {
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
    } else if (pos >= (BLOCKSIZE - 1)) {
      val ob = block(BLOCKSIZE + 1).asInstanceOf[Array[AnyRef]]
      if (ob eq null) {
        val nb = new Array[AnyRef](BLOCKSIZE + 4)
        nb(0) = block(BLOCKSIZE - 1)
        nb(BLOCKSIZE) = End
        CAS(BLOCKSIZE + 1, ob, nb)
      }
      // TODO we have a race here
      block = block(BLOCKSIZE + 1).asInstanceOf[Array[AnyRef]]
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
    case el: CBElem[T] =>
      el.elem(obj)
      applyCBs(el.next, obj)
    case _ =>
  }

}


sealed class CBList[-T]


final class CBElem[-T] (
  val elem: T => Any,
  val next: CBList[T]
) extends CBList[T]


final object CBNil extends CBList[Any]


object End


private object Seal



