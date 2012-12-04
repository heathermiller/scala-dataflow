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

  override protected type SubJob = FAFlatMapJob[A,B]

  protected def subCopy(s: Int, e: Int) = 
    new FAFlatMapJob(src, dst, f, n, offset, s, e, thresh, this)

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

  import FAJob.JobGen

  def apply[A : ClassManifest, B : ClassManifest](
    dst: HierFlowArray[B],
    f: A => FlowArray[B],
    n: Int
  ) = new JobGen[A] {
    def apply(src: FlatFlowArray[A], dstOffset: Int, srcOffset: Int, length: Int) =
      new FAFlatMapJob(src, dst, f, n, dstOffset, srcOffset,
                       srcOffset + length - 1, FAJob.threshold(length), null)
  }

}
