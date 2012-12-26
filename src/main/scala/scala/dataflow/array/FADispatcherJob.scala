package scala.dataflow.array

import scala.reflect.ClassTag

private[array] class FADispatcherJob[A : ClassTag] private (
  val src: HierFlowArray[A],
  val d: FAJob.JobGen[A],
  val offset: Int,
  start: Int,
  end: Int,
  thr: Int,
  obs: FAJob.Observer
) extends FAJob(start, end, thr, obs) {

  override protected type SubJob = FADispatcherJob[A]

  protected def subCopy(s: Int, e: Int) = 
    new FADispatcherJob(src, d, offset, s, e, thresh, this)

  protected def doCompute() {
    val n = src.subSize
    val sJobs =
      for ( (i,l,u) <- src.subSlices(start, end) )
        yield src.subData(i).dispatch(d, offset + i * n + l, l, u - l + 1)

    delegate(sJobs)
  }

  override def sliceJobs(from: Int, to: Int) = {
    if (!d.needDeepJobSearch) {
      super.sliceJobs(from, to)
    } else if (isSplit) {
      val (j1, j2) = subTasks
      SlicedJob.mergeDeps(
        j1.sliceJobs(from, to),
        j2.sliceJobs(from, to)
      )
    } else if (isDelegated) {
      delegates map { d =>
        SlicedJob.mergeDeps(d.map(_.sliceJobs(from, to)) :_*)
      } getOrElse None
    } else if (done) {
      // We are fully done already
      None
    } else {
      // Nothing started yet. Wait for this job.
      Some(Vector(this), true)
    }
  }

}

object FADispatcherJob {

  def apply[A : ClassTag](
    src: HierFlowArray[A],
    d: FAJob.JobGen[A],
    dstOffset: Int,
    srcOffset: Int,
    length: Int
  ): FADispatcherJob[A] =
      new FADispatcherJob(src, d, dstOffset - srcOffset, srcOffset,
                          srcOffset + length - 1, FAJob.threshold(length), null)

}
