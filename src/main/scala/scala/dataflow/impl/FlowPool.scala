package scala.dataflow.impl



import scala.annotation.tailrec
import scala.dataflow.FlowPoolLike
import jsr166y._
import scala.dataflow.Future

class FlowPool[T <: AnyRef] {

  import FlowPool._
  
  val initBlock = FlowPool.newBlock
  
  def builder: Builder[T] = new Builder[T](initBlock)

  def foreach[U](f: T => U) = {
    val fut = new Future[Unit]()
    (new CBWriter(initBlock)).addCB(f, fut)
    fut
  }

  def folding[U <: T](acc: U)(cmb: (T, U) => U) = {

  }

}

object FlowPool {

  private val BLOCKSIZE = 256
  
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
    private def advance(cb: T => Any, endf: Future[Unit]) {
      val pos = lastpos
      val obj = block(pos)
      if (obj eq Seal) {
        endf.complete()
        return
      }
      if (!obj.isInstanceOf[CBList[_]]) {
        cb(obj.asInstanceOf[T])
        lastpos = pos + 1
        advance(cb, endf)
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
    def addCB(cb: T => Any, endf: Future[Unit]) {
      if (lastpos < BLOCKSIZE) {
        val pos = lastpos
        val curo = block(pos)
        if (curo.isInstanceOf[CBList[T]]) {
          val no = new CBElem(cb, endf, curo.asInstanceOf[CBList[T]])
          if (!CAS(pos, curo, no)) addCB(cb, endf)
        } else {
          advance(cb, endf)
          addCB(cb, endf)
        }
      } else {
        advance(cb, endf)
        addCB(cb, endf)
      }
    }
  }
  
  val forkjoinpool = new ForkJoinPool
  
  def task[T](fjt: ForkJoinTask[T]) = Thread.currentThread match {
    case fjw: ForkJoinWorkerThread =>
      fjt.fork()
    case _ =>
      forkjoinpool.execute(fjt)
  }
  
}


final class Builder[T <: AnyRef](bl: Array[AnyRef]) {

  @volatile private var lastpos = 0
  @volatile private var block   = bl

  private val unsafe = getUnsafe()
  private val ARRAYOFFSET = unsafe.arrayBaseOffset(classOf[Array[AnyRef]])
  private val ARRAYSTEP   = unsafe.arrayIndexScale(classOf[Array[AnyRef]])
  @inline private def RAWPOS(idx: Int) = ARRAYOFFSET + idx * ARRAYSTEP
  @inline private def CAS(idx: Int, exp: Any, x: Any) =
    unsafe.compareAndSwapObject(block, RAWPOS(idx), exp, x)
  
  private def BLOCKSIZE = 256
  
  @tailrec
  def <<(x: T): this.type = {
    val pos = lastpos
    val npos = pos + 1
    val next = block(npos)
    val curo = block(pos)
    if (curo.isInstanceOf[CBList[T]] && ((next eq null) || next.isInstanceOf[CBList[_]])) {
      if (CAS(npos, next, curo)) {
        if (CAS(pos, curo, x)) {
          lastpos = npos
          applyCBs(curo.asInstanceOf[CBList[T]], block, pos)
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
  private def applyCBs[T](e: CBList[T], block: Array[AnyRef], pos: Int): Unit = e match {
    case el: CBElem[T] =>
      el.awakeCallback(block, pos)
      applyCBs(el.next, block, pos)
    case _ =>
  }

}


sealed class CBList[-T]


final class CBElem[-T] (
  val func: T => Any,
  val endf: Future[Unit],
  val next: CBList[T]
) extends CBList[T] {
  // if the count is negative, then
  // there is no active batch task
  // if the count is positive, then
  // the there is an active batch task
  // that will handle calling callbacks
  // for any elements which haven't been called
  @volatile var count: Int = -1
  
  def awakeCallback(block: Array[AnyRef], pos: Int) {
    val cnt = /*READ*/count
    if (cnt < 0) {
      // there is no active batch
      if (tryOwn(cnt)) {
        // we are now responsible for starting the active batch
        // so we start a new fork-join task to call the callbacks
        FlowPool.task(new CBElem.BatchTask(block, pos, this))
      }
    }
  }
  
  def tryOwn(cnt: Int): Boolean = CBElem.CAS_COUNT(this, cnt, -cnt)
  
  def unown(cnt: Int) = CBElem.WRITE_COUNT(this, cnt)
}


object CBElem {
  
  val unsafe = getUnsafe()
  
  val COUNTOFFSET = unsafe.objectFieldOffset(classOf[CBElem[_]].getDeclaredField("head"))
  
  def CAS_COUNT(obj: CBElem[_], ov: Int, nv: Int) = unsafe.compareAndSwapObject(obj, COUNTOFFSET, ov, nv)
  
  def WRITE_COUNT(obj: CBElem[_], v: Int) = unsafe.putObjectVolatile(obj, COUNTOFFSET, v)
  
  final class BatchTask[T](var block: Array[AnyRef], var startpos: Int, val callback: CBElem[T]) extends RecursiveAction {
    @tailrec
    protected def compute() {
      // invoke callback while there are elements
      var cnt = callback.count
      var pos = startpos
      var cur = block(pos)
      while ((cur ne null) && !cur.isInstanceOf[CBList[_]] && (cur ne Seal) && (cur ne End)) {
        callback.func(cur.asInstanceOf[T])
        pos += 1
        cnt += 1
        cur = block(pos)
      }

      // TODO Alex verify this is OK and free all references
      if (cur eq Seal) {
        callback.endf.complete()
      }
      
      // relinquish control
      callback.unown(-cnt)
      
      // see if there are more elements available
      // if there are, try to regain control and start again
      cur = block(pos)
      if ((cur ne null) && !cur.isInstanceOf[CBList[_]] && (cur ne Seal) && (cur ne End)) {
        if (callback.tryOwn(-cnt)) {
          startpos = pos
          compute()
        }
      }
    }
  }
  
}


final object CBNil extends CBList[Any]


object End


private object Seal



