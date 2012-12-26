package scala.dataflow.array

import scala.dataflow.Future
import scala.annotation.tailrec
import scala.reflect.ClassTag

class FlowArraySliceView[A : ClassTag](
  private val data: ConcreteFlowArray[A],
  private val offset: Int,
  val size: Int
) extends FlowArray[A] with FAJob.Observer {

  import FlowArray._
  import FlowArraySliceView._
  import SlicedJob._

  /// Internals ///
  @inline private def CAS(ov: State, nv: State) =
    FlowArraySliceView.unsafe.compareAndSwapObject(this, OFFSET, ov, nv)

  @volatile private var alignState: State = Unknown

  @inline
  final private def tryStartAlign() {
    val job = data.align(this.offset + offset, size)
    if (CAS(Unknown, Running(job))) {
      // We made THE job
      job.addObserver(this)
      FAJob.schedule(job)
    }
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
    new FlowArraySliceView(data, offset + start, end - start)

  def flatten[B](n: Int)(implicit flat: CanFlatten[A,B], mf: ClassTag[B]): FlowArray[B] =
    // TODO: This can be slower than necessary
    data.flatten(n).slice(offset, offset + size - 1)

  def fold[A1 >: A](from: Int, to: Int)(z: A1)(op: (A1, A1) => A1): FoldFuture[A1] =
    data.fold[A1](offset + from, offset + to)(z)(op)

  override def transpose(from: Int, to: Int)(step: Int) =
    data.transpose(offset + from, offset + to)(step)

  def zipMapFold[B : ClassTag, C](from: Int, to: Int)(that: FlowArray[B])(f: (A,B) => C)(z: C)(op: (C,C) => C) = data.zipMapFold(offset + from, offset + to)(that)(f)(z)(op)

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

  private val unsafe = getUnsafe()
  private val OFFSET =
    unsafe.objectFieldOffset(classOf[FlowArraySliceView[_]].getDeclaredField("alignState"))

  abstract class State
  case object Unknown extends State
  case object Done extends State
  case class Running(job: FAAlignJob) extends State

}
