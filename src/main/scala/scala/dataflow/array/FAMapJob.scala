package scala.dataflow.array

private[array] class FAMapJob[A : ClassManifest, B : ClassManifest] private (
  val src: FlatFlowArray[A],
  val dst: FlatFlowArray[B],
  val f: A => B,
  val offset: Int,
  start: Int,
  end: Int,
  thr: Int,
  obs: FAJob.Observer
) extends FAJob(start, end, thr, obs) {

  protected def subCopy(s: Int, e: Int) = 
    new FAMapJob(src, dst, f, offset, s, e, thresh, this)

  protected def doCompute() {
    for (i <- start to end) {
      dst.data(i + offset) = f(src.data(i))
    }
  }

}

object FAMapJob {

  def apply[A : ClassManifest, B : ClassManifest](
    src: FlatFlowArray[A],
    dst: FlatFlowArray[B],
    f: A => B) =
      new FAMapJob(src, dst, f, 0, 0, src.size - 1, FAJob.threshold(src.size), dst)

  def apply[A : ClassManifest, B : ClassManifest](
    src: FlatFlowArray[A],
    dst: FlatFlowArray[B],
    f: A => B,
    offset: Int) =
      new FAMapJob(src, dst, f, offset, 0, src.size - 1, FAJob.threshold(src.size), dst)

}
