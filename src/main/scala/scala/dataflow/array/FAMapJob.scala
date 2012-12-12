package scala.dataflow.array

private[array] class FAMapJob[A : ClassManifest, B : ClassManifest] private (
  val src: FlatFlowArray[A],
  val dst: FlatFlowArray[B],
  val f: A => B,
  offset: Int,
  start: Int,
  end: Int,
  thr: Int,
  obs: FAJob.Observer
) extends DestFAJob[B](offset, start, end, thr, obs) {

  override protected type SubJob = FAMapJob[A,B]

  protected def subCopy(s: Int, e: Int) = 
    new FAMapJob(src, dst, f, offset, s, e, thresh, this)

  protected def doCompute() {
    for (i <- start to end) {
      dst.data(i + offset) = f(src.data(i))
    }
  }

}

object FAMapJob {

  import FAJob.JobGen

  def apply[A : ClassManifest, B : ClassManifest](
    dst: FlatFlowArray[B],
    f: A => B
  ) = new JobGen[A] {
    def apply(src: FlatFlowArray[A], dstOffset: Int, srcOffset: Int, length: Int) =
      new FAMapJob(src, dst, f, dstOffset - srcOffset, srcOffset,
                   srcOffset + length - 1, FAJob.threshold(length), null)
  }

}
