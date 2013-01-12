package scala.dataflow.array

import scala.dataflow.Future
import scala.annotation.tailrec
import scala.reflect.ClassTag

/**
 * View of a FA that exposes just a slice
 */
class FlowArraySliceView[A : ClassTag] private[array] (
  /** underlying FA */
  private val data: ConcreteFlowArray[A],
  /** offset of slice */
  private val offset: Int,
  /** size of slice */
  val size: Int
) extends FlowArray[A] with FAJob.Observer {

  import FlowArray._
  import FlowArraySliceView._
  import SlicedJob._

  /// Internals ///

  /** CAS align state */
  @inline
  private def CAS(ov: State, nv: State) =
    FlowArraySliceView.unsafe.compareAndSwapObject(this, OFFSET, ov, nv)

  /**
   * state of alignment of this FASV
   *
   * may be:
   * - Unknown (no alignment)
   * - Done (this FASV is fully calculated)
   * - Running(j) (FASV is calculating, j can be used for dependency tracking)
   */
  @volatile
  private var alignState: State = Unknown

  /**
   * try to start an align job
   *
   * after returning from this method, `alignState` will not be
   * `Unknown` anymore
   */
  @inline
  final private def tryStartAlign() {
    val job = data.align(this.offset + offset, size)
    if (CAS(Unknown, Running(job))) {
      // We made THE job
      job.addObserver(this)
      FAJob.schedule(job)
    }
  }

  override private[array] def copyToArray(
      dst: Array[A],
      srcPos: Int,
      dstPos: Int,
      length: Int) {
    data.copyToArray(dst, srcPos + offset, dstPos, length)
  }

  @tailrec
  override private[array] final def sliceJobs(from: Int, to: Int) = {
    /*READ*/alignState match {
      case Unknown =>
        tryStartAlign()
        sliceJobs(from, to)
      case Done =>
        None
      case Running(j) =>
        val js = j.destSliceJobs(from, to)
        Some(js, false)
    }
  }

  /**
   * Dispatch the jobs
   *
   * Note: we cannot use the job in alignState for dependency
   * management since we do not know the structure of underlying FA
   * and hence are unable to actually create the jobs
   */
  private[array] def dispatch(gen: JobGen, dstOffset: Int, srcOffset: Int, length: Int) =
    data.dispatch(gen, dstOffset, srcOffset + offset, math.min(length, size))

  @tailrec
  override private[array] final def tryAddObserver(obs: FAJob.Observer) = {
    /*READ*/alignState match {
      case Unknown =>
        tryStartAlign()
      tryAddObserver(obs)
      case Done => false
      case Running(j) => j.tryAddObserver(obs)
    }
  }

  @tailrec
  override final def done: Boolean = /*READ*/alignState match {
    case Unknown =>
      tryStartAlign()
      done
    case Done => true
    case Running(j) => j.done
  }

  override def slice(start: Int, end: Int): FlowArray[A] =
    new FlowArraySliceView(data, offset + start, end - start)

  override def flatten[B](n: Int)(implicit flat: CanFlatten[A,B],
                                  mf: ClassTag[B]): FlowArray[B] =
    // TODO: This can be slower than necessary
    data.flatten(n).slice(offset, offset + size - 1)

  override def fold[A1 >: A](from: Int, to: Int)
                            (z: A1)
                            (op: (A1, A1) => A1): FoldFuture[A1] =
    data.fold[A1](offset + from, offset + to)(z)(op)

  override def transpose(from: Int, to: Int)(step: Int) =
    data.transpose(offset + from, offset + to)(step)

  override def zipMapFold[B : ClassTag, C](from: Int, to: Int) 
                                          (that: FlowArray[B])
                                          (f: (A,B) => C)
                                          (z: C)
                                          (op: (C,C) => C) =
    data.zipMapFold(offset + from, offset + to)(that)(f)(z)(op)

  override def jobDone() {
    /*WRITE*/alignState = Done
    freeBlocked()
  }

  override def unsafe(i: Int) = data.unsafe(i + offset)

  override def blocking(isAbs: Boolean, msecs: Long) = {
    block(isAbs, msecs)

    val ret = new Array[A](size)
    copyToArray(ret, 0, 0, size)
    ret
  }

}

object FlowArraySliceView {

  private val unsafe = getUnsafe()
  private val OFFSET =
    unsafe.objectFieldOffset(classOf[FlowArraySliceView[_]].getDeclaredField("alignState"))

  /** align state of FASV */
  abstract class State
  /** initial state */
  case object Unknown extends State
  /** FASV has completed */
  case object Done extends State
  /** An AlignJob is running */
  case class Running(job: FAAlignJob) extends State

}
