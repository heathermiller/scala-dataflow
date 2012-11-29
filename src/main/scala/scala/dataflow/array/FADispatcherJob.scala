package scala.dataflow.array

private[array] class FADispatcherJob[A : ClassManifest] private (
  val src: HierFlowArray[A],
  val d: FAJob.JobGen[A],
  val offset: Int,
  start: Int,
  end: Int,
  thr: Int,
  obs: FAJob.Observer
) extends FAJob(start, end, thr, obs) {

  protected def subCopy(s: Int, e: Int) = 
    new FADispatcherJob(src, d, offset, s, e, thresh, this)

  protected def doCompute() {
    val sJobs =
      for (i <- start to end)
      yield src.subData(i).dispatch(d, offset + i*src.subSize)

    delegate(sJobs)
  }

}

object FADispatcherJob {

  def apply[A : ClassManifest](
    src: HierFlowArray[A],
    d: FAJob.JobGen[A],
    offset: Int): FADispatcherJob[A] =
      new FADispatcherJob(src, d, offset, 0, src.outerSize - 1,
                          FAJob.threshold(src.outerSize), null)

}
