package scala.dataflow.array

private[array] class FATransformJob[A : ClassManifest, B : ClassManifest] private (
  val src: FlowArray[A],
  val dst: FlowArray[B],
  val f: A => B,
  start: Int,
  end: Int,
  thr: Int,
  obs: FAJob.Observer
) extends FAJob(start, end, thr, obs) {

  def this(src: FlowArray[A], dst: FlowArray[B], f: A => B) =
    this(src, dst, f, 0, src.size - 1, FAJob.threshold(src.size), dst)

  protected def subCopy(s: Int, e: Int) = 
    new FATransformJob(src, dst, f, s, e, thresh, this)

  protected def doCompute() {
    for (i <- start to end) {
      dst.data(i) = f(src.data(i))
    }
  }

}

object FATransformJob {

  def apply[A : ClassManifest, B : ClassManifest](
    src: FlowArray[A],
    dst: FlowArray[B],
    f: A => B) = new FATransformJob(src, dst, f)

}
