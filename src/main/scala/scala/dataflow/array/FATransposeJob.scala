package scala.dataflow.array

import scala.dataflow.Future
import scala.reflect.ClassTag

/**
 * Transposes a FA
 */
private[array] class FATransposeJob[A : ClassTag] private (
  /** source of data */
  val src: FlatFlowArray[A],
  /** destination of data */
  val dst: FlatFlowArray[A],
  /** step in between source blocks, interpret as one dimension */
  val step: Int,
  /** offset of the view that requested transpose */
  val viewOffset: Int,
  /** size of the view that requested transpose */
  val viewSize: Int,
  /** offset in destination for writing */
  val dstOffset: Int,
  start: Int,
  end: Int,
  thr: Int,
  obs: FAJob.Observer
) extends FAJob(start, end, thr, obs) {

  override protected type SubJob = FATransposeJob[A]

  /** size of destination blocks, interpret as other dimension */
  val blSize = viewSize / step

  /** source -> target index */
  @inline
  private def ti(iS: Int) = {
    val i = iS - viewOffset + dstOffset
    (i / step) + (i % step) * blSize
  }

  /** target -> source index */
  @inline
  private def si(iT: Int) = {
    (iT / blSize) + (iT % blSize) * step + viewOffset - dstOffset
  }

  override protected def subCopy(s: Int, e: Int) = 
    new FATransposeJob(src, dst, step, viewOffset, viewSize,
                       dstOffset, s, e, thresh, this)

  override protected def doCompute() {
    for (i <- start to end) {
      dst.data(ti(i)) = src.data(i)
    }
  }

  /** this thing does not really cover any range */
  protected override def covers(from: Int, to: Int) = false

  /** deeply inspect the job tree to find true dependencies. */
  override def destSliceJobs(from: Int, to: Int): Vector[FATransposeJob[A]] = {
    if (isSplit) {
      val (j1,j2) = subTasks
      j1.destSliceJobs(from, to) ++ j2.destSliceJobs(from, to)
    } else if (!done) {
      val myr = start to end
      val inds = (from to to).map(i => si(i) - viewOffset)
      if (inds.exists(myr.contains _)) Vector(this)
      else Vector()
    } else { Vector() }
  }

}

private[array] object FATransposeJob {

  import FAJob.JobGen

  /** create new JobGen that creates transpose jobs */
  def apply[A : ClassTag](
    dst: FlatFlowArray[A],
    step: Int,
    viewOffset: Int,
    viewLength: Int
  ) = new JobGen[A] {
    override val needDeepJobSearch = true
    def apply(src: FlatFlowArray[A], dstOffset: Int, srcOffset: Int, length: Int) =
      // TODO have a better size threshold
      new FATransposeJob(src, dst, step, viewOffset, viewLength, dstOffset, srcOffset,
                         srcOffset + length - 1, FAJob.threshold(length), null)
  }

}
