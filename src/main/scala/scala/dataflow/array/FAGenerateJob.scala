package scala.dataflow.array

private[array] class FAGenerateJob[A : ClassManifest] private (
  val dst: FlatFlowArray[A],
  val f: Int => A,
  start: Int,
  end: Int,
  thr: Int,
  obs: FAJob.Observer
) extends FAJob(start, end, thr, obs) {

  override protected type SubJob = FAGenerateJob[A]

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
    dst: FlatFlowArray[A],
    f: Int => A) =
      new FAGenerateJob(dst, f, 0, dst.size - 1, FAJob.threshold(dst.size), dst)

}
