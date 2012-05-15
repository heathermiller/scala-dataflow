package scala.dataflow



import scala.annotation.tailrec
import jsr166y._



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
  @volatile private var position = Next(bl)
  
  private val unsafe = getUnsafe()
  private val ARRAYOFFSET      = unsafe.arrayBaseOffset(classOf[Array[AnyRef]])
  private val ARRAYSTEP        = unsafe.arrayIndexScale(classOf[Array[AnyRef]])
  private val BLOCKFIELDOFFSET = unsafe.objectFieldOffset(classOf[Builder[_]].getDeclaredField("position"))
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
        if (CAS(curblock, pos, curo, x)) {
          p.index = npos
          applyCallbacks(curo.asInstanceOf[CallbackList[T]], curblock, pos)
          this
        } else <<(x)
      } else <<(x)
    } else {
      if (tryAdd(x)) this
      else <<(x)
    }
  }
  
  def seal(size: Int) {
    // TODO
  }
  
  private def tryAdd(x: T): Boolean = {
    import FlowPool._
    def goToNext(curblock: Array[AnyRef], oldposition: Next, next: Next) {
      CAS_BLOCK_PTR(oldposition, next)
    }
    def expand(curblock: Array[AnyRef], me: MustExpand) {
      val at = MUST_EXPAND_POS
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
    
    val p = /*READ*/position
    val curblock = p.block
    val pos = /*READ*/p.index
    val obj = curblock(pos)
    obj match {
      case Seal(sz) => // flowpool sealed here - error
        sys.error("Insert on a sealed structure.")
      case me @ MustExpand(_) => // must extend with a new block
        expand(curblock, me)
      case ne @ Next(_) => // the next block already exists - go to it
        goToNext(curblock, p, ne)
      case cbs: CallbackList[_] => // a list of callbacks here - check if this is the end of the block
        curblock(pos + 1) match {
          case me @ MustExpand(_) =>
            expand(curblock, me)
          case Seal(sz) =>
            if (CAS(curblock, pos, cbs, x)) {
              p.index = pos + 1
              applyCallbacks(cbs.asInstanceOf[CallbackList[T]], curblock, pos)
              return true
            }
          case ne @ Next(_) =>
            goToNext(curblock, p, ne)
          case _ =>
        }
      case _ => // a regular object - advance
        p.index = pos + 1
        tryAdd(x)
    }
    false
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
  val endf: Int => Any,
  val next: CallbackList[T],
  var block: Array[AnyRef],
  var pos: Int
) extends CallbackList[T] {
  @volatile var lock: Int = -1
  
  @tailrec
  def awakeCallback(block: Array[AnyRef], pos: Int) {
    val lk = /*READ*/lock
    if (lk < 0) {
      // there is no active batch
      if (tryOwn()) {
        // we are now responsible for starting the active batch
        // so we start a new fork-join task to call the callbacks
        FlowPool.task(new CallbackElem.BatchTask(this))
      } else awakeCallback(block, pos)
    }
  }
  
  def tryOwn(): Boolean = CallbackElem.CAS_COUNT(this, -1, 1)
  
  def unOwn() = CallbackElem.WRITE_COUNT(this, -1)
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
  
  final class BatchTask[T](val callback: CallbackElem[T]) extends RecursiveAction {
    import FlowPool._
    
    @tailrec
    protected def compute() {
      if (callback.pos >= LAST_CALLBACK_POS) {
        callback.block(MUST_EXPAND_POS) match {
          case MustExpand(sealedAt) => // don't do anything
          case Next(b) =>
            callback.block = b
            callback.pos = 0
        }
      }
      
      // invoke callback while there are elements or we reach an end of the block
      val block = callback.block
      var pos = callback.pos
      var cur = block(pos)
      while (!cur.isInstanceOf[CallbackList[_]] && !cur.isInstanceOf[Seal]) {
        callback.func(cur.asInstanceOf[T])
        pos += 1
        cur = block(pos)
      }
      callback.pos = pos
      
      // relinquish control
      callback.unOwn()
      
      // see if there are more elements available
      // if there are, try to regain control and start again
      cur = block(pos)
      cur match {
        case cb: CallbackList[_] =>
        case Seal(sz) =>
          callback.endf(sz)
        case _ =>
          if (callback.tryOwn()) {
            if (callback.pos < LAST_CALLBACK_POS) compute()
            else FlowPool.task(new CallbackElem.BatchTask(callback))
          }
      }
    }
  }
  
}


final object CallbackNil extends CallbackList[Any]


case class Seal(size: Int)


case class MustExpand(sealedAt: Int) {
  def isSealed = sealedAt >= 0 
}


case class Next(val block: Array[AnyRef]) {
  @volatile var index: Int = 0
}



