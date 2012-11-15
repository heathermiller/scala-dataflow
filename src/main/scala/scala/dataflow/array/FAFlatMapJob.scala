package scala.dataflow.array

private[array] class FAFlatMapJob[A : ClassManifest, B : ClassManifest] private (
  val src: FlatFlowArray[A],
  val dst: HierFlowArray[B],
  val f: A => FlowArray[B],
  val n: Int,
  start: Int,
  end: Int,
  thr: Int,
  obs: FAJob.Observer
) extends FAJob(start, end, thr, obs) {

  protected def subCopy(s: Int, e: Int) = 
    new FAFlatMapJob(src, dst, f, n, s, e, thresh, this)

  protected def doCompute() {
    for (i <- start to end) {
      val sub = f(src.data(i))
      assert(sub.size == n)
      dst.subData(i) = sub
    }
  }

}

object FAFlatMapJob {

  def apply[A : ClassManifest, B : ClassManifest](
    src: FlatFlowArray[A],
    dst: HierFlowArray[B],
    f: A => FlowArray[B],
    n: Int) =
      new FAFlatMapJob(src, dst, f, n, 0, src.size - 1, FAJob.threshold(src.size), dst)

}
