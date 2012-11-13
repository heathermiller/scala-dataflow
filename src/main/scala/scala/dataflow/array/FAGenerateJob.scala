package scala.dataflow.array

private[array] class FAGenerateJob[A : ClassManifest] private (
  val dst: FlowArray[A],
  val f: Int => A,
  start: Int,
  end: Int,
  thr: Int,
  obs: FAJob.Observer
) extends FAJob(start, end, thr, obs) {

  protected def subCopy(s: Int, e: Int) = 
    new FAGenerateJob(dst, f, s, e, thresh, this)

  protected def doCompute() {
    for (i <- start to end) {
      dst.data(i) = f(i)
    }
  }

}

object FAGenerateJob {

  def apply[A : ClassManifest](
    dst: FlowArray[A],
    f: Int => A,
    obs: FAJob.Observer) =
      new FAGenerateJob(dst, f, 0, dst.size - 1, FAJob.threshold(dst.size), dst)

}
