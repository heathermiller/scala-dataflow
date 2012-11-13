package scala.dataflow.array

private[array] class FACopyJob[A : ClassManifest] private (
  val src: FlatFlowArray[A],
  val dst: FlatFlowArray[A],
  val offset: Int,
  start: Int,
  end: Int,
  thr: Int,
  obs: FAJob.Observer
) extends FAJob(start, end, thr, obs) {

  protected def subCopy(s: Int, e: Int) = 
    new FACopyJob(src, dst, offset, s, e, thresh, this)

  protected def doCompute() {
    for (i <- start to end) {
      dst.data(i+offset) = src.data(i)
    }
  }

}

object FACopyJob {

  def apply[A : ClassManifest](
    src: FlatFlowArray[A],
    dst: FlatFlowArray[A],
    offset: Int,
    obs: FAJob.Observer) =
      new FACopyJob(src, dst, offset, 0, src.size - 1, FAJob.threshold(src.size), obs)

}
