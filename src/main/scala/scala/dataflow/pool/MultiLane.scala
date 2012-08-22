package scala.dataflow
package pool



import scala.annotation.tailrec
import jsr166y._



final class MultiLane[T](val lanes: Int) extends FlowPool[T] {

  import FlowPool._
  import MultiLane._
  
  protected lazy val directBuilder = builder
  val initBlocks = Array.fill(lanes)(newBlock(0, CallbackNil))
  val sealHolder = new MLSealHolder()
  
  def +=(elem: T) = {
    directBuilder += elem
    this
  }
  
  def seal(sz: Int) = directBuilder.seal(sz)
  
  def newPool[S] = new MultiLane[S](lanes)

  def builder = new MultiLane.Builder[T](initBlocks, sealHolder)

  def aggregate[S](zero: =>S)(cmb: (S, S) => S)(folder: (S, T) => S): Future[S] = {
    val a = FlowLatch[S](zero)(cmb)
    a.seal(lanes)
    
    for (b <- initBlocks) {
      val cbe = new CallbackElem[T, S](folder, (sz, acc) => a << acc, CallbackNil, b, 0, zero)
      task(new RegisterCallbackTask(cbe))
    }
    
    a.future
  }
  
}


object MultiLane extends Factory[MultiLane] {
  
  def apply[T]() = new MultiLane(Runtime.getRuntime.availableProcessors)

  final class Builder[T](
    bls: Array[Array[AnyRef]],
    sealHolder: MLSealHolder
  ) extends scala.dataflow.Builder[T] {

    import MLSealHolder._

    @volatile private var positions = bls.map(bl => Next(bl))
    private val lanes = bls.length

    @volatile private var hasher: MLHasher = null
    
    private val unsafe = getUnsafe()
    private val ARRAYOFFSET      = unsafe.arrayBaseOffset(classOf[Array[AnyRef]])
    private val ARRAYSTEP        = unsafe.arrayIndexScale(classOf[Array[AnyRef]])
    @inline private def RAWPOS(idx: Int) = ARRAYOFFSET + idx * ARRAYSTEP
    @inline private def CAS(bl: Array[AnyRef], idx: Int, ov: AnyRef, nv: AnyRef) =
      unsafe.compareAndSwapObject(bl, RAWPOS(idx), ov, nv)
    private val SH_OFFSET = unsafe.objectFieldOffset(
      classOf[MLSealHolder].getDeclaredField("s"))
    @inline private def CAS_SH(ov: State, nv: State) = 
      unsafe.compareAndSwapObject(sealHolder, SH_OFFSET, ov, nv)
    
    @tailrec
    def +=(x: T): this.type = {
      val bli = getblocki
      val p = /*READ*/positions(bli)
      val curblock = p.block
      val pos = /*READ*/p.index
      val npos = pos + 1
      val next = curblock(npos)
      val curo = curblock(pos)

      if (curo.isInstanceOf[CallbackList[_]] && ((next eq null) || next.isInstanceOf[CallbackList[_]])) {
        if (CAS(curblock, npos, next, curo)) {
          if (CAS(curblock, pos, curo, x.asInstanceOf[AnyRef])) {
            p.index = npos
            applyCallbacks(curo.asInstanceOf[CallbackList[T]], curblock)
            this
          } else +=(x)
        } else +=(x)
      } else {
        if (tryAdd(x,bli)) this
        else +=(x)
      }
    }
    
    @tailrec
    def seal(size: Int) {
      val st = /*READ*/sealHolder.s
      st match {
        case Unsealed => {
          val ns = Proposition(size)
          if (!CAS_SH(st, ns)) seal(size)
          else {
            // Check if we succeeded. SealTags will be cleaned up by writers
            if (!helpSeal(ns)) sys.error("Seal failed")
          }
        }
        case p: Proposition => {
          // Another seal is running. Help it finish. If it succeeds, this one fails.
          // Otherwise --> retry
          if (helpSeal(p)) sys.error("Seal failed")
          else seal(size)
        }
        case MLSeal(sz,_) if size != sz =>
          sys.error("already sealed at %d (!= %d)".format(sz, size))
        case _ => // Sealed with same size: Done
      }

    }

    /**
     * helps sealing (any thread may call this)
     * @return true if succeess, false if failure
     */
    private def helpSeal(p: Proposition): Boolean = {
      var sizes: Int = 0
      
      for ((pos,bli) <- positions zipWithIndex) {
        sealTag(p, bli, /*READ*/pos.block, /*READ*/pos.index) match {
          case Left(v) => sizes = sizes + v
          case Right(success) => return success
        }
      }

      if (sizes <= p.size) {
        // At this point we know that the seal MUST succeed
        val rem = p.size - sizes
        val nv = new MLSeal(p.size, rem)
        if (CAS_SH(p, nv)) finalizeSeals(rem)
        true
      } else {
        // At this point we know that the seal MUST fail
        CAS_SH(p, Unsealed)
        false
      }

    }
    
    @tailrec
    private def sealTag(
        p: Proposition,
        bli: Int,
        curblock: Array[AnyRef],
        pos: Int
      ): Either[Int,Boolean] = {

      import FlowPool._

      curblock(pos) match {
        case MustExpand =>
          expand(curblock, bli)
          sealTag(p, bli, curblock, pos)
        case Next(block) =>
          sealTag(p, bli, block, 0)
        case cbl: CallbackList[_] =>
          if (pos < LAST_CALLBACK_POS) {
            val cnt = totalElems(curblock, pos)
            val nv = SealTag(p, cbl)
            if (cnt > p.size) Right(false)
            else if (CAS(curblock, pos, cbl, nv)) Left(cnt)
            else sealTag(p, bli, curblock, pos)
          } else sealTag(p, bli, curblock, pos + 1)
        case _: Seal[_] => Right(true)
        case ov @ SealTag(op, cbs: CallbackList[T]) =>
          val cnt = totalElems(curblock, pos)
          if (op eq p) Left(cnt)
          else {
            /*READ*/sealHolder.s match {
              case gpr: Proposition =>
                if (gpr ne p) Right(false)
                else {
                  val nv = SealTag[T](p, cbs)
                  if (cnt > p.size) Right(false)
                  else if (CAS(curblock, pos, ov, nv)) Left(cnt)
                  else sealTag(p, bli, curblock, pos)
                }
              case _: MLSeal => Right(true)
              case Unsealed => Right(false)
            }
          }
        case _ =>
          sealTag(p, bli, curblock, pos + 1)
      }
    }

    private def sealSize(bli: Int, rem: Int) =
      rem / lanes + (if (bli < rem % lanes) 1 else 0)
    
    private def totalElems(curblock: Array[AnyRef], pos: Int) = {
      import FlowPool._
      val blockidx = curblock(IDX_POS).asInstanceOf[Int]
      blockidx * MAX_BLOCK_ELEMS + pos
    }
    
    private def goToNext(next: Next, bli: Int) {
      positions(bli) = next // ok - not racey
    }
    
    private def expand(curblock: Array[AnyRef], bli: Int) {
      import FlowPool._

      val at = MUST_EXPAND_POS
      val curidx = curblock(IDX_POS).asInstanceOf[Int]

      // Prepare new block with CBs
      val nextblock = newBlock(curidx + 1,curblock(at - 1))
      
      val next = Next(nextblock)
      if (CAS(curblock, at, MustExpand, next)) {
        // take a shortcut here
        goToNext(next, bli)
      }
    }
    
    private def tryAdd(x: T, bli: Int): Boolean = {
      import FlowPool._

      val p = /*READ*/positions(bli)
      val curblock = p.block
      val pos = /*READ*/p.index
      val obj = curblock(pos)

      obj match {
        case Seal(sz, null) =>
          // Rehash. We don't care about races
          val h = /*READ*/hasher
          if (h eq null) hasher = new MLHasher(lanes)
          else if (h.advance > lanes) {
            return findFreeBlock map {
              fbli => tryAdd(x, fbli)
            } getOrElse sys.error("Insert on a sealed structure.")
          }
        case MustExpand =>
          // must extend with a new block
          expand(curblock, bli)
        case ne @ Next(_) =>
          // the next block already exists - go to it
          goToNext(ne, bli)
        case t: SealTag[_] =>
          // Need to remove tag (i.e. finish or abort seal)
          tryResolveTag(t, curblock, pos, bli)
        case cbh: CallbackHolder[_] =>
          // a list of callbacks here - check if this is the end of the block
          val nextelem = curblock(pos + 1)
          nextelem match {
            case MustExpand =>
              expand(curblock, bli)
            case ne @ Next(_) =>
              goToNext(ne, bli)
            case _: CallbackHolder[_] | null =>
              // current is Seal(sz, _ != null), next is not at the end
              // check size and append
              val curelem = curblock(pos)
              curelem match {
                case Seal(sz, cbs) =>
                  val total = totalElems(curblock, pos)
                  val nseal = if (total < (sz - 1)) curelem else Seal(sz, null)
                  if (CAS(curblock, pos + 1, nextelem, nseal)) {
                    if (CAS(curblock, pos, curelem, x.asInstanceOf[AnyRef])) {
                      p.index = pos + 1
                      applyCallbacks(cbs, curblock)
                      return true
                    }
                  }
                case _ =>
              }
            case _ => // Someone has written
          }
        case _ => // a regular object - advance
          p.index = pos + 1
      }
      false
    }

    private def finalizeSeals(rem: Int) {

      @tailrec
      def finalize(curblock: Array[AnyRef], pos: Int, bli: Int) {
        curblock(pos) match {
          case Seal(_, null) =>
          case null =>
          case MustExpand =>
          case os @ SealTag(_, cbs) =>
            val cnt = totalElems(curblock, pos)
            val sz = sealSize(bli, rem)
            val ns = os.toSeal(cnt, sz)
            if (CAS(curblock, pos, os, ns)) {
              if (sz == 0) applyCallbacks(cbs, curblock)
            } else finalize(curblock, pos, bli)
          case Next(block) =>
            finalize(block, 0, bli)
          case _ =>
            finalize(curblock, pos + 1, bli)
        }
      }

      for ((p/*READ*/,i) <- positions zipWithIndex) {
        finalize(p.block, /*READ*/p.index, i)
      }

    }

    private def findFreeBlock = {
      // This only checks the current block. No advancing is done
      @tailrec
      def isFree(curblock: Array[AnyRef], pos: Int): Boolean = {
        /*READ*/curblock(pos) match {
          case Seal(_, null) => false
          case _: CallbackHolder[_] => true
          case _ => isFree(curblock, pos+1)
        }
      }

      positions.zipWithIndex find {
        case (/*READ*/p, bli) => 
          val curblock = p.block
          val pos = /*READ*/p.index
          isFree(curblock,pos)
      } map (_._2)
    }

    private def tryResolveTag[T](t: SealTag[T], curblock: Array[AnyRef], pos: Int, bli: Int) {
      val st = /*READ*/sealHolder.s
      st match {
        case p: Proposition if (p eq t.p) =>
          // Still sealing --> help
          helpSeal(p)
        case MLSeal(_,rem) =>
          // Seal succeeded --> confirm tag
          val cnt = totalElems(curblock, pos)
          val sz = sealSize(bli, rem)
          val ns = t.toSeal(cnt, sz)
          if (CAS(curblock, pos, t, ns) && sz == 0)
            applyCallbacks(t.callbacks, curblock)
        case _ =>
          // Something failed --> revert tag
          CAS(curblock, pos, t, t.callbacks)
      }
    }
    
    @tailrec
    private def applyCallbacks[T](e: CallbackList[T], b: Array[AnyRef]): Unit = e match {
      case el: CallbackElem[T, _] =>
        el.pollCallback(b)
        applyCallbacks(el.next, b)
      case _ =>
    }

    /**
     * gets index of block index to use, based on current thread
     */
    private def getblocki = {
      val tid = Thread.currentThread.getId
      val h = /*READ*/hasher
      if (h eq null)
        (tid % lanes).asInstanceOf[Int]
      else
        h.getblocki(tid)
    }
    
  }

}
