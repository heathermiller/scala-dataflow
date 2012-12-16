package scala.dataflow.array

import scala.dataflow.Future
import scala.annotation.tailrec

class FlowArraySliceView[A : ClassManifest](
  private val data: ConcreteFlowArray[A],
  private val offset: Int,
  val size: Int
) extends FlowArray[A] with FAJob.Observer {

  import FlowArray._
  import FlowArraySliceView._

  /// Internals ///
  
  private val unsafe = getUnsafe()
  private val OFFSET =
    unsafe.objectFieldOffset(classOf[FlowArraySliceView[_]].getDeclaredField("alignState"))
  @inline private def CAS(ov: State[A], nv: State[A]) =
    unsafe.compareAndSwapObject(this, OFFSET, ov, nv)

  @volatile private var alignState: State[A] = Unknown

  @inline
  final private def tryStartAlign() {
    val job = align(0, size)
    if (CAS(Unknown, Running(job))) {
      // We made THE job
      job.addObserver(this)
      FAJob.schedule(job)
    }
  }

  private[array] def align(offset: Int, size: Int) = {
    assert(offset + size <= this.size)
    data.align(this.offset + offset, size)
  }

  private[array] def copyToArray(dst: Array[A], srcPos: Int, dstPos: Int, length: Int) {
    data.copyToArray(dst, srcPos + offset, dstPos, length)
  }

  @tailrec
  private[array] final def sliceJobs(from: Int, to: Int) = /*READ*/alignState match {
    case Unknown =>
      tryStartAlign()
      sliceJobs(from, to)
    case Done =>
      None
    case Running(j) =>
      val js = j.destSliceJobs(from, to)
      Some(js, false)
  }

  /**
   * Dispatch the jobs
   *
   * Note: we cannot actually use the job in alignState for dependency management since
   * we do not know the underlying structure and hence are unable to actually create the
   * jobs
   */
  private[array] def dispatch(gen: JobGen, dstOffset: Int, srcOffset: Int, length: Int) =
    data.dispatch(gen, dstOffset, srcOffset + offset, math.min(length, size))

  @tailrec
  final private[array] def tryAddObserver(obs: FAJob.Observer) = /*READ*/alignState match {
    case Unknown =>
      tryStartAlign()
      tryAddObserver(obs)
    case Done => false
    case Running(j) => j.tryAddObserver(obs)
  }

  /** Checks if this job is done */
  @tailrec
  final def done: Boolean = /*READ*/alignState match {
    case Unknown =>
      tryStartAlign()
      done
    case Done => true
    case Running(j) => j.done
  }

  override def slice(start: Int, end: Int): FlowArray[A] =
    new FlowArraySliceView(data, offset + start, end - start + 1)

  def flatten[B](n: Int)(implicit flat: CanFlatten[A,B], mf: ClassManifest[B]): FlowArray[B] =
    // TODO: This can be slower than necessary
    data.flatten(n).slice(offset, offset + size - 1)

  def fold[A1 >: A](from: Int, to: Int)(z: A1)(op: (A1, A1) => A1): FoldFuture[A1] =
    data.fold[A1](offset, offset + size - 1)(z)(op)

  override def jobDone() {
    /*WRITE*/alignState = Done
    freeBlocked()
  }

  def unsafe(i: Int) = data.unsafe(i + offset)
  def blocking(isAbs: Boolean, msecs: Long) = {
    block(isAbs, msecs)

    val ret = new Array[A](size)
    copyToArray(ret, 0, 0, size)
    ret
  }

}

object FlowArraySliceView {

  abstract class State[+A]
  case object Unknown extends State[Nothing]
  case object Done extends State[Nothing]
  case class Running[A](job: FAAlignJob[A]) extends State[A]

}