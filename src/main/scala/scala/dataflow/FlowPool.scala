package scala.dataflow



import scala.annotation.tailrec
import jsr166y._



class FlowPool[T <: AnyRef] {

  import FlowPool._
  
  val initBlock = FlowPool.newBlock(0)
  
  def builder: Builder[T] = new Builder[T](initBlock)

  def doForAll[U](f: T => U): Future[Int] = {
    val fut = new Future[Int]()
    val cbe = new CallbackElem(f, fut.complete _, CallbackNil, initBlock, 0)
    task(new RegisterCallbackTask(cbe))
    fut
  }

  def mappedFold[U, V <: U](accInit: V)(cmb: (U,V) => V)(map: T => U): Future[(Int, V)] = {
    /* We do not need to synchronize on this var, because IN THE
     * CURRENT SETTING, callbacks are only executed in sequence.
     * This WILL break if the scheduling changes
     */
    var acc = accInit

    doForAll { x =>
      acc = cmb(map(x), acc)
    } map {
      c => (c,acc)
    }
  }

  // Monadic Ops

  def map[S <: AnyRef](f: T => S): FlowPool[S] = {
    val fp = new FlowPool[S]
    val b  = fp.builder

    doForAll { x =>
      b << f(x)
    } map { b.seal _ }

    fp
  }

  def foreach[U](f: T => U) { doForAll(f) }

  def filter(f: T => Boolean): FlowPool[T] = {
    val fp = new FlowPool[T]
    val b  = fp.builder

    mappedFold(0)(_ + _) { x =>
      if (f(x)) { b << x; 1 }
      else 0
    }  map {
      case (c,fc) => b.seal(fc)
    }

    fp
  }

  def flatMap[S <: AnyRef](f: T => FlowPool[S]): FlowPool[S] = {
    val fp = new FlowPool[S]
    val b  = fp.builder

    mappedFold(future(0))(futLift(_ + _)) { x =>
      val ifp = f(x)
      ifp.doForAll(b << _)
    } map { 
      case (c,cfut) => cfut.map(b.seal _)
    }

    fp
  }

}



object FlowPool {

  def BLOCKSIZE = 256
  def LAST_CALLBACK_POS = BLOCKSIZE - 3
  def MUST_EXPAND_POS = BLOCKSIZE - 2
  def IDX_POS = BLOCKSIZE - 1
  def MAX_BLOCK_ELEMS = LAST_CALLBACK_POS
  
  // TODO refactor to use that everywhere (or change to initBlock) -- tobias
  def newBlock(idx: Int) = {
    val bl = new Array[AnyRef](BLOCKSIZE)
    bl(0) = CallbackNil
    bl(MUST_EXPAND_POS) = MustExpand()
    bl(IDX_POS) = idx.asInstanceOf[AnyRef]
    bl
  }
  
  val forkjoinpool = new ForkJoinPool
  
  def task[T](fjt: ForkJoinTask[T]) = Thread.currentThread match {
    case fjw: ForkJoinWorkerThread =>
      fjt.fork()
    case _ =>
      forkjoinpool.execute(fjt)
  }

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
        case s: Seal[_] if totalElems >= s.size => cb.endf(s.size)
        // At end of current elements
        case cbh: CallbackHolder[T] => {
          val newel = cbh.insertCB(cb)
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
        case s: Seal[_] if totalElems >= s.size => {
          cb.endf(s.size)
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
        case me: MustExpand => {
          val curidx = cb.block(IDX_POS).asInstanceOf[Int]
          val nextblock = new Array[AnyRef](BLOCKSIZE)
          val curblock = cb.block

          // prepare callback to be added
          cb.block = nextblock
          cb.pos = 0

          // prepare next block
          nextblock(0) = curcb.asInstanceOf[CallbackHolder[_]].insertCB(cb)
          nextblock(MUST_EXPAND_POS) = MustExpand()
          nextblock(IDX_POS) = (curidx + 1).asInstanceOf[AnyRef]
    
          // Swap block in an end.
          if (CAS(curblock, MUST_EXPAND_POS, me, Next(nextblock))) return

          // We failed CASing. We have another Next now. Update and move on
          cb.block = curblock(MUST_EXPAND_POS).asInstanceOf[Next].block
          cb.pos = 0
          
        }
        case _ => sys.error("FlowPool block in inconsistent state: " +
                            "Unknown object at MUST_EXPAND_POS. Epic " +
                            "Fail you DIE (miserably).") 
      }

      task(new RegisterCallbackTask(cb))
      
    }

    private def totalElems = {
      val blockidx = cb.block(IDX_POS).asInstanceOf[Int]
      blockidx * MAX_BLOCK_ELEMS + cb.pos
    }

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
    //println(curblock, pos)
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
    val p = /*READ*/position
    val curblock = p.block
    val pos = /*READ*/p.index
    seal(size, curblock, pos)
  }
  
  @tailrec
  private def seal(size: Int, curblock: Array[AnyRef], pos: Int) {
    curblock(pos) match {
      case me @ MustExpand() =>
        expand(curblock, me)
      case Next(block) =>
        seal(size, block, 0)
      case cbl: CallbackList[_] =>
        if (pos < FlowPool.LAST_CALLBACK_POS) {
          val total = totalElems(curblock, pos)
          val nseal =
            if (total < size) Seal(size, cbl)
            else if (total == size) Seal(size, null)
            else sys.error("sealing with %d < number of elements in flow-pool %d".format(size, total))
          if (!CAS(curblock, pos, cbl, nseal)) seal(size, curblock, pos)
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
  
  private def goToNext(curblock: Array[AnyRef], oldposition: Next, next: Next) {
    //CAS_BLOCK_PTR(oldposition, next)
    position = next // ok - not racey
  }
  
  private def expand(curblock: Array[AnyRef], me: MustExpand) {
    import FlowPool._
    val at = MUST_EXPAND_POS
    val curidx = curblock(IDX_POS).asInstanceOf[Int]
    val nextblock = new Array[AnyRef](BLOCKSIZE)
    // copy callbacks to next block
    nextblock(0) = curblock(at - 1)
    nextblock(MUST_EXPAND_POS) = MustExpand()
    nextblock(IDX_POS) = (curidx + 1).asInstanceOf[AnyRef]
    
    CAS(curblock, at, me, Next(nextblock))
  }
  
  
  private def tryAdd(x: T): Boolean = {
    import FlowPool._
    
    val p = /*READ*/position
    val curblock = p.block
    val pos = /*READ*/p.index
    val obj = curblock(pos)
    //println("tryAdd", curblock, pos, obj)
    obj match {
      case Seal(sz, null) => // flowpool sealed here - error
        sys.error("Insert on a sealed structure.")
      case me @ MustExpand() => // must extend with a new block
        expand(curblock, me)
      case ne @ Next(_) => // the next block already exists - go to it
        goToNext(curblock, p, ne)
      case cbh: CallbackHolder[_] => // a list of callbacks here - check if this is the end of the block
        //println("callbackholder")
        val nextelem = curblock(pos + 1)
        nextelem match {
          case me @ MustExpand() =>
            expand(curblock, me)
          case ne @ Next(_) =>
            goToNext(curblock, p, ne)
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
                    applyCallbacks(cbh.callbacks, curblock, pos)
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
  private def applyCallbacks[T](e: CallbackList[T], block: Array[AnyRef], pos: Int): Unit = e match {
    case el: CallbackElem[T] =>
      el.awakeCallback(block, pos)
      applyCallbacks(el.next, block, pos)
    case _ =>
  }
  
}


trait CallbackHolder[-T] {
  def callbacks: CallbackList[T]
  def insertCB[U <: T](el: CallbackElem[U]): CallbackHolder[U]
}


sealed class CallbackList[-T] extends CallbackHolder[T] {
  def callbacks = this
  def insertCB[U <: T](el: CallbackElem[U]): CallbackList[U] =
    new CallbackElem(
      el.func,
      el.endf,
      this,
      el.block,
      el.pos
    )
}


final class CallbackElem[-T] (
  val func: T => Any,
  val endf: Int => Any,
  val next: CallbackList[T],
  var block: Array[AnyRef],
  var pos: Int
) extends CallbackList[T] {
  @volatile var lock: Int = -1
  
  /* ATTENTION:
   * If you change the scheduling, make sure that FlowPool.mappedFold
   * synchronized still properly. Otherwise there will be races.
   */
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
          case MustExpand() => // don't do anything
          case Next(b) =>
            callback.block = b
            callback.pos = 0
        }
      }
      
      // invoke callback while there are elements or we reach an end of the block
      val block = callback.block
      var pos = callback.pos
      var cur = block(pos)
      while (!cur.isInstanceOf[CallbackList[_]] && !cur.isInstanceOf[Seal[_]]) {
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
        case Seal(sz, null) =>
          callback.endf(sz)
        case Seal(sz, cbs) =>
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


case class Seal[T](size: Int, callbacks: CallbackList[T]) extends CallbackHolder[T] {
  def insertCB[U <: T](el: CallbackElem[U]) = Seal(size, callbacks.insertCB(el))
}


case class MustExpand()


case class Next(val block: Array[AnyRef]) {
  @volatile var index: Int = 0
}



