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

}

object FADispatcherJob {

  def apply[A : ClassManifest](
    src: HierFlowArray[A],
    d: FAJob.JobGen[A],
    dstOffset: Int,
    srcOffset: Int,
    length: Int
  ): FADispatcherJob[A] =
      new FADispatcherJob(src, d, dstOffset - srcOffset, srcOffset,
                          srcOffset + length - 1, FAJob.threshold(length), null)

}
