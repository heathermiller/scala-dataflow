package scala.dataflow

import scala.annotation.tailrec
import jsr166y._

final class MultiLaneBuilder[T](
  bls: Array[Array[AnyRef]],
  sealHolder: MLSealHolder
) extends Builder[T] {

  import MLSealHolder._

  @volatile private var positions = bls.map(bl => Next(bl))
  
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
  def <<(x: T): this.type = {
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
          applyCallbacks(curo.asInstanceOf[CallbackList[T]])
          this
        } else <<(x)
      } else <<(x)
    } else {
      if (tryAdd(x,bli)) this
      else <<(x)
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
      case MLSeal(sz) if size != sz =>
        sys.error("already sealed at %d (!= %d)".format(sz, size))
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
      if (CAS_SH(p, nv) && rem == 0)
        finalizeSeals 
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
      case Seal(sz, _) =>
        /*READ*/sealHolder.s match {
          case MLSeal(ssz) => Right(sz == ssz)
          case _ => 
            sys.error("MultiLaneFlowPool in inconsistent" +
                      "state. (Seal found without global seal)")
        }
      case ov @ SealTag(op, cbl) =>
        val cnt = totalElems(curblock, pos)
        if (op eq p) Left(cnt)
        else {
          /*READ*/sealHolder.s match {
            case gpr: Proposition =>
              if (gpr ne p) Right(false)
              else {
                val nv = SealTag(p, cbl)
                if (cnt > p.size) Right(false)
                else if (CAS(curblock, pos, ov, nv)) Left(cnt)
                else sealTag(p, bli, curblock, pos)
              }
            case MLSeal(sz) => 
              val nv = ov.toSeal(cnt)
              if (CAS(curblock, pos, ov, nv)) Right(true)
              else sealTag(p, bli, curblock, pos)
            case Unsealed => Right(false)
          }
        }
      case _ =>
        sealTag(p, bli, curblock, pos + 1)
    }
  }
  
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
  
  @tailrec
  private def tryAdd(x: T, bli: Int): Boolean = {
    import FlowPool._

    val p = /*READ*/positions(bli)
    val curblock = p.block
    val pos = /*READ*/p.index
    val obj = curblock(pos)

    obj match {
      case Seal(sz, null) =>
        // FlowPool sealed here - take another block
        findFreeBlock match {
          case Some(fbli) => tryAdd(x, fbli)
          case None => sys.error("Insert on a sealed structure.")
        }
      case os @ StealSeal(sz, cbs) =>
        val gs = /*READ*/sealHolder.s.asInstanceOf[MLSeal]
        val stealState = gs.stageSteal(bli)
        val stC = stealState.stolen(bli)
        val ns = os.stolen(stC)

        if (CAS(curblock, pos, os, ns)) {
          gs.commitSteal(bli)
          if (stC > 0 && stealState.rem == 0)
            // We stole the last count
            finalizeSeals
        }
        tryAdd(x,bli)
      case MustExpand =>
        // must extend with a new block
        expand(curblock, bli); false
      case ne @ Next(_) =>
        // the next block already exists - go to it
        goToNext(ne, bli); false
      case t: SealTag[_] =>
        // Need to remove tag (i.e. finish or abort seal)
        resolveTag(t, curblock, pos); false
      case cbh: CallbackHolder[_] =>
        // a list of callbacks here - check if this is the end of the block
        val nextelem = curblock(pos + 1)
        nextelem match {
          case MustExpand =>
            expand(curblock, bli); false
          case ne @ Next(_) =>
            goToNext(ne, bli); false
          case _: CallbackHolder[_] | null =>
            // current is Seal(sz, _ != null), next is not at the end
            // check size and append
            val curelem = curblock(pos)
            val total = totalElems(curblock, pos)
            curelem match {
              case os @ Seal(sz, cbs) if sz <= total =>
                // Prepare stealing some slots
                val ns = os.stealing
                CAS(curblock, pos, os, ns)
                tryAdd(x,bli)
              case Seal(sz, cbs) =>
                if (CAS(curblock, pos + 1, nextelem, curelem)) {
                  if (CAS(curblock, pos, curelem, x.asInstanceOf[AnyRef])) {
                    p.index = pos + 1
                    applyCallbacks(cbs)
                    true
                  } else false
                } else false
              case NoStealSeal(sz, cbs) =>
                val nseal = if (total < (sz - 1)) curelem else Seal(sz, null)
                if (CAS(curblock, pos + 1, nextelem, nseal)) {
                  if (CAS(curblock, pos, curelem, x.asInstanceOf[AnyRef])) {
                    p.index = pos + 1
                    applyCallbacks(cbs)
                    true
                  } else false
                } else false
              case _ => false
            }
          case _ => false // Someone has written
        }
      case _ => // a regular object - advance
        p.index = pos + 1
        false
    }
  }

  private def finalizeSeals {

    @tailrec
    def finalize(curblock: Array[AnyRef], pos: Int) {
      curblock(pos) match {
        case Seal(_, null) =>
        case NoStealSeal(_,_) =>
        case null =>
        case MustExpand =>
        case os @ Seal(sz, cbs) if sz <= totalElems(curblock, pos) =>
          if (!CAS(curblock, pos, os, Seal(sz, null)))
            finalize(curblock, pos)
          else
            applyCallbacks(cbs)
        case os @ Seal(sz, cbs) =>
          if (!CAS(curblock, pos, os, os.noStealing))
            finalize(curblock, pos)
        case os @ StealSeal(sz, cbs) =>
          if (!CAS(curblock, pos, os, Seal(sz, null)))
            finalize(curblock, pos)
          else
            applyCallbacks(cbs)
        case os @ SealTag(_, cbs) =>
          val sz = totalElems(curblock, pos)
          if (!CAS(curblock, pos, os, Seal(sz, null)))
            finalize(curblock, pos)
          else
            applyCallbacks(cbs)
        case Next(block) =>
          finalize(block,0)
        case _ =>
          finalize(curblock, pos + 1)
      }
    }

    positions foreach { /*READ*/p => finalize(p.block, /*READ*/p.index) }

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

  private def resolveTag[T](t: SealTag[T], curblock: Array[AnyRef], pos: Int) {
    val st = /*READ*/sealHolder.s
    st match {
      case p: Proposition if (p eq t.p) =>
        // Still sealing --> help
        helpSeal(p)
      case MLSeal(_) => 
        // Seal succeeded --> confirm tag
        CAS(curblock, pos, t, t.toSeal(totalElems(curblock, pos)))
      case _ =>
        // Something failed --> revert tag
        CAS(curblock, pos, t, t.callbacks)
    }
  }
  
  @tailrec
  private def applyCallbacks[T](e: CallbackList[T]): Unit = e match {
    case el: CallbackElem[T] =>
      el.awakeCallback()
      applyCallbacks(el.next)
    case _ =>
  }

  /**
   * gets index of block index to use, based on current thread
   */
  private def getblocki = {
    (Thread.currentThread.getId % positions.length).asInstanceOf[Int]
  }
  
}
