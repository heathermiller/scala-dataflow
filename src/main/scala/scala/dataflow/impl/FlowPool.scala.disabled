package scala.dataflow.impl



import scala.annotation.tailrec
import jsr166y._
import scala.dataflow.Future
import scala.dataflow.future



class FlowPool[T <: AnyRef] {

  import FlowPool._
  
  val initBlock = FlowPool.newBlock(0)
  
  def builder: Builder[T] = new Builder[T](initBlock)

  // def foreach[U](f: T => U): Future[Int] = {
  //   val fut = new Future[Int]()
  //   (new CallbackWriter(initBlock)).addCallback(f, fut)
  //   fut
  // }

  final class RegisterCallbackTask[T](var block: Array[AnyRef], val callback: CallbackElem[T]) extends RecursiveAction {
    @tailrec
    def compute() {
      compute()
    }
  }
  
  def folding[V,U <: V](acc: U)(cmb: (V, U) => U) =
    new FoldingFlowPool(this, acc, cmb)

  // def map[S <: AnyRef](f: T => S): FlowPool[S] = {
  //   val fp = new FlowPool[S]
  //   val b  = fp.builder

  //   {
  //     for (x <- this) { b << f(x) }
  //   } map { b.seal _ }

  //   fp
  // }

  // def filter(f: T => Boolean): FlowPool[T] = {
  //   val fp = new FlowPool[T]
  //   val b  = fp.builder

  //   {
  //     for (x <- this.folding(0)(_ + _)) {
  //       if (f(x)) { b << x; 1 }
  //       else 0
  //     }
  //   } map { b.seal _ }

  //   fp
  // }

  // def flatMap[S <: AnyRef](f: T => FlowPool[S]): FlowPool[S] = {
  //   val fp = new FlowPool[S]
  //   val b  = fp.builder

  //   def fsum(f1: Future[Int], f2: Future[Int]) = f1.flatMap(x => f2.map(x + _))

  //   {
  //     for (x <- this.folding(future(0))(fsum _)) {
  //       val ifp = f(x)
  //       for (y <- ifp) { b << y }
  //     }
  //   } map { _.map(b.seal _) }

  //   fp
  // }

}

object FlowPool {

  def BLOCKSIZE = 256
  def LAST_CALLBACK_POS = BLOCKSIZE - 4
  def MUST_EXPAND_POS = BLOCKSIZE - 3
  def IDX_POS = BLOCKSIZE - 2
  def LOCK_POS = BLOCKSIZE - 1
  def MAX_BLOCK_ELEMS = LAST_CALLBACK_POS
  
  def newBlock(idx: Int) = {
    val bl = new Array[AnyRef](BLOCKSIZE)
    bl(0) = CallbackNil
    bl(MUST_EXPAND_POS) = MustExpand(-1)
    bl(IDX_POS) = idx.asInstanceOf[AnyRef]
    bl
  }
  
  /*
  private final class CallbackWriter[T](bl: Array[AnyRef]) {
    
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
      if (obj.isInstanceOf[Seal]) {
        // TODO Heather find size of Seal. store in seal?
        endf.complete(0)
        return
      }
      if (!obj.isInstanceOf[CallbackList[_]]) {
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
    def addCallback(cb: T => Any, endf: Future[Int]) {
      if (lastpos < BLOCKSIZE) {
        val pos = lastpos
        val curo = block(pos)
        if (curo.isInstanceOf[CallbackList[T]]) {
          val no = new CallbackElem(cb, endf, curo.asInstanceOf[CallbackList[T]])
          if (!CAS(pos, curo, no)) addCallback(cb, endf)
        } else {
          advance(cb, endf)
          addCallback(cb, endf)
        }
      } else {
        advance(cb, endf)
        addCallback(cb, endf)
      }
    }
  }
  */
  
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
  private val ARRAYOFFSET      = unsafe.arrayBaseOffset(classOf[Array[AnyRef]])
  private val ARRAYSTEP        = unsafe.arrayIndexScale(classOf[Array[AnyRef]])
  private val BLOCKFIELDOFFSET = unsafe.objectFieldOffset(classOf[Builder[_]].getDeclaredField("block"))
  @inline private def RAWPOS(idx: Int) = ARRAYOFFSET + idx * ARRAYSTEP
  @inline private def CAS(bl: Array[AnyRef], idx: Int, ov: AnyRef, nv: AnyRef) =
    unsafe.compareAndSwapObject(bl, RAWPOS(idx), ov, nv)
  def CAS_BLOCK_PTR(ov: Array[AnyRef], nv: Array[AnyRef]) =
    unsafe.compareAndSwapObject(this, BLOCKFIELDOFFSET, ov, nv)
  
  @tailrec
  def <<(x: T): this.type = {
    val firstread = block
    val pos = lastpos
    val npos = pos + 1
    val curblock = block
    val next = curblock(npos)
    val curo = curblock(pos)
    if ((firstread eq curblock) && curo.isInstanceOf[CallbackList[_]] && ((next eq null) || next.isInstanceOf[CallbackList[_]])) {
      if (CAS(curblock, npos, next, curo)) {
        if (CAS(curblock, pos, curo, x)) {
          lastpos = npos
          applyCallbacks(curo.asInstanceOf[CallbackList[T]], curblock, pos)
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
  
  private def advance() {
    import FlowPool._
    def goToNext(curblock: Array[AnyRef], nextblock: Array[AnyRef]) {
      // to avoid races - CAS block in builder from curblock to nextblock
      // and if successful lastpos = 0 (racey, but affects perf. rather than correctness)
      // if (CAS_BLOCK_PTR(curblock, nextblock)) lastpos = 0 -> hm, slow for some reason...
      // ok, then spin, spin like you've never spinned before
      while (!CAS(curblock, LOCK_POS, null, new AnyRef)) {}
      block = nextblock 
      lastpos = 0
      curblock(LOCK_POS) = null
    }
    def expand(at: Int, curblock: Array[AnyRef], me: MustExpand) {
      val curidx = curblock(IDX_POS).asInstanceOf[Int]
      val nextblock = new Array[AnyRef](BLOCKSIZE)
      // copy callbacks to next block
      nextblock(0) = curblock(at - 1)
      // try to seal or propagate seal information
      if (me.isSealed && me.sealedAt <= (curidx + 2) * (MAX_BLOCK_ELEMS)) {
        val sealpos = me.sealedAt - (curidx + 1) * (MAX_BLOCK_ELEMS)
        nextblock(sealpos) = Seal(me.sealedAt)
      } else {
        nextblock(MUST_EXPAND_POS) = MustExpand(me.sealedAt)
      }
      nextblock(IDX_POS) = (curidx + 1).asInstanceOf[AnyRef]
      
      CAS(curblock, at, me, Next(nextblock))
    }
    
    val curblock = block
    val pos = lastpos
    val obj = curblock(pos)
    obj match {
      case Seal(sz) => // flowpool sealed here - error
        sys.error("Insert on a sealed structure.")
      case me @ MustExpand(_) => // must extend with a new block
        expand(pos, curblock, me)
      case Next(nextblock) => // the next block already exists - go to it
        goToNext(curblock, nextblock)
      case cbs: CallbackList[_] => // a list of callbacks here - check if this is the end of the block
        curblock(pos + 1) match {
          case me @ MustExpand(_) =>
            expand(pos + 1, curblock, me)
          case Seal(sz) =>
            // TODO special slow add goes here
          case Next(nextblock) =>
            goToNext(curblock, nextblock)
          case _ =>
        }
      case _ => // a regular object - advance
        lastpos = pos + 1
        advance()
    }
  }
  
  @tailrec
  private def applyCallbacks[T](e: CallbackList[T], block: Array[AnyRef], pos: Int): Unit = e match {
    case el: CallbackElem[T] =>
      el.awakeCallback(block, pos)
      applyCallbacks(el.next, block, pos)
    case _ =>
  }
  
}


sealed class CallbackList[-T]


final class CallbackElem[-T] (
  val func: T => Any,
  val endf: T => Any,
  val next: CallbackList[T]
) extends CallbackList[T] {
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
        FlowPool.task(new CallbackElem.BatchTask(block, pos, this))
      }
    }
  }
  
  def tryOwn(cnt: Int): Boolean = CallbackElem.CAS_COUNT(this, cnt, -cnt)
  
  def unown(cnt: Int) = CallbackElem.WRITE_COUNT(this, cnt)
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

    // { for (v <- pool) { acc = cmb(f(v),acc) }
    // } map { ign => acc }
    null
  }

}

object CallbackElem {
  
  val unsafe = getUnsafe()
  
  val COUNTOFFSET = unsafe.objectFieldOffset(classOf[CallbackElem[_]].getDeclaredField("head"))
  
  def CAS_COUNT(obj: CallbackElem[_], ov: Int, nv: Int) = unsafe.compareAndSwapObject(obj, COUNTOFFSET, ov, nv)
  
  def WRITE_COUNT(obj: CallbackElem[_], v: Int) = unsafe.putObjectVolatile(obj, COUNTOFFSET, v)
  
  final class BatchTask[T](var block: Array[AnyRef], var startpos: Int, val callback: CallbackElem[T]) extends RecursiveAction {
    @tailrec
    protected def compute() {
      // invoke callback while there are elements or we reach an end of the block
      var cnt = callback.count
      var pos = startpos
      var cur = block(pos)
      while ((cur ne null) && !cur.isInstanceOf[CallbackList[_]]) {
        callback.func(cur.asInstanceOf[T])
        pos += 1
        cnt += 1
        cur = block(pos)
      }
      
      // TODO Alex verify this is OK and free all references
      // TODO Heather find size of seal
      // if (cur.isInstanceOf[Seal]) {
      //   callback.endf.complete(0)
      // }
      
      // relinquish control
      callback.unown(-cnt)
      
      // see if there are more elements available
      // if there are, try to regain control and start again
      cur = block(pos)
      if ((cur ne null) && !cur.isInstanceOf[CallbackList[_]]) {
        if (callback.tryOwn(-cnt)) {
          startpos = pos
          compute()
        }
      }
    }
  }
  
}


final object CallbackNil extends CallbackList[Any]


trait BlockEnd


case class Seal(size: Int) extends BlockEnd


case class MustExpand(sealedAt: Int) extends BlockEnd {
  def isSealed = sealedAt >= 0
}


case class Next(block: Array[AnyRef]) extends BlockEnd



