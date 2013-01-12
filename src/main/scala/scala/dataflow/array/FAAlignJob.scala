package scala.dataflow.array

/**
 * Re-align indices on a given `SlicedJob`. This job should not be
 * scheduled through the `dispatch` framework but launched
 * directly. It will wait on its dependants itself (which is the only
 * thing it does)
 */
private[array] class FAAlignJob private (
  val srcJob: SlicedJob,
  start: Int,
  end: Int,
  thr: Int,
  obs: FAJob.Observer
) extends FAJob(start, end, thr, obs) {

  override protected type SubJob = FAAlignJob

  override protected def subCopy(s: Int, e: Int) =
    new FAAlignJob(srcJob, s, e, thresh, this)

  override protected def doCompute() {
    srcJob.sliceJobs(start, end) match {
      // No need to retry after
      case Some((j, false)) => delegate(j)
      // Need to retry after
      case Some((j, true))  => delegateThen(j) { doCompute _ }
      // All done
      case None =>
    }
  }

}

private[array] object FAAlignJob {

  /** create new FAAlignJob */
  def apply(src: SlicedJob, start: Int, end: Int) =
    new FAAlignJob(src, start, end, FAJob.threshold(end - start + 1), null)

}
