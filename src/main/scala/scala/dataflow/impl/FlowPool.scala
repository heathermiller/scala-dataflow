package scala.dataflow.impl



import scala.annotation.tailrec
import scala.dataflow.FlowPoolLike
import jsr166y._
import scala.dataflow.Future
import scala.dataflow.future

class FlowPool[T <: AnyRef] {

  import FlowPool._
  
  val initBlock = FlowPool.newBlock
  
  def builder: Builder[T] = new Builder[T](initBlock)

  def foreach[U](f: T => U): Future[Int] = {
    val fut = new Future[Int]()
    (new CBWriter(initBlock)).addCB(f, fut)
    fut
  }

  def folding[V,U <: V](acc: U)(cmb: (V, U) => U) =
    new FoldingFlowPool(this, acc, cmb)

  def map[S <: AnyRef](f: T => S): FlowPool[S] = {
    val fp = new FlowPool[S]
    val b  = fp.builder

    {
      for (x <- this) { b << f(x) }
    } map { b.seal _ }

    fp
  }

  def filter(f: T => Boolean): FlowPool[T] = {
    val fp = new FlowPool[T]
    val b  = fp.builder

    {
      for (x <- this.folding(0)(_ + _)) {
        if (f(x)) { b << x; 1 }
        else 0
      }
    } map { b.seal _ }

    fp
  }

  def flatMap[S <: AnyRef](f: T => FlowPool[S]): FlowPool[S] = {
    val fp = new FlowPool[S]
    val b  = fp.builder

    def fsum(f1: Future[Int], f2: Future[Int]) = f1.flatMap(x => f2.map(x + _))

    {
      for (x <- this.folding(future(0))(fsum _)) {
        val ifp = f(x)
        for (y <- ifp) { b << y }
      }
    } map { _.map(b.seal _) }

    fp
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
    private def advance(cb: T => Any, endf: Future[Int]) {
      val pos = lastpos
      val obj = block(pos)
      if (obj eq Seal) {
        // TODO Heather find size of Seal. store in seal?
        endf.complete(0)
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
    def addCB(cb: T => Any, endf: Future[Int]) {
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
  
  def seal(size: Int) {
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
  val endf: Future[Int],
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

final class FoldingFlowPool[T <: AnyRef, V, U <: V](
  pool: FlowPool[T],
  accInit: U,
  cmb: (V, U) => U
) {
  
  def foreach(f: T => V): Future[U] = {
    // TODO races here!
    // TODO ALEX can the scheduler take care of this or do we need to sync?
    var acc = accInit

    { for (v <- pool) { acc = cmb(f(v),acc) }
    } map { ign => acc }
  }

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
      // TODO Heather find size of seal
      if (cur eq Seal) {
        callback.endf.complete(0)
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



