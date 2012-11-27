package scala.dataflow.array

private[array] class FAFlatMapJob[A : ClassManifest, B : ClassManifest] private (
  val src: FlatFlowArray[A],
  val dst: HierFlowArray[B],
  val f: A => FlowArray[B],
  val n: Int,
  val offset: Int,
  start: Int,
  end: Int,
  thr: Int,
  obs: FAJob.Observer
) extends FAJob(start, end, thr, obs) {

  protected def subCopy(s: Int, e: Int) = 
    new FAFlatMapJob(src, dst, f, n, offset, s, e, thresh, this)

  // TODO: in a HFA, this job is marked as the final one, but not
  // really finalizing all the work. Hence waiting on this job will
  // not be sufficient to call unsafe!

  protected def doCompute() {
    for (i <- start to end) {
      val sub = f(src.data(i))
      assert(n == sub.size)
      dst.subData(i + offset) = sub
    }
  }

  override protected def covers(from: Int, to: Int) = {
    val is = dst.subSize
    super.covers(from/is, to/is)
  }

}

object FAFlatMapJob {

  def apply[A : ClassManifest, B : ClassManifest](
    src: FlatFlowArray[A],
    dst: HierFlowArray[B],
    f: A => FlowArray[B],
    n: Int,
    of: Int) =
      new FAFlatMapJob(src, dst, f, n, of, 0, src.size - 1, FAJob.threshold(src.size), null)

}
