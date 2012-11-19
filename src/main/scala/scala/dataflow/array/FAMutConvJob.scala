package scala.dataflow.array

private[array] class FAMutConvJob[A : ClassManifest] private (
  val src: FlatFlowArray[A],
  val dst: FlatFlowArray[A],
  val f: A => Unit,
  val cond: A => Boolean,
  val offset: Int,
  start: Int,
  end: Int,
  thr: Int,
  obs: FAJob.Observer
) extends FAJob(start, end, thr, obs) {

  protected def subCopy(s: Int, e: Int) = 
    new FAMutConvJob(src, dst, f, cond, offset, s, e, thresh, this)

  protected def doCompute() {
    for (i <- start to end) {
      var x = src.data(i)
      while (!cond(x)) { f(x) }
      dst.data(i + offset) = x
    }
  }

}

object FAMutConvJob {

  def apply[A : ClassManifest](
    src: FlatFlowArray[A],
    dst: FlatFlowArray[A],
    f: A => Unit,
    cond: A => Boolean,
    offset: Int
  ) = new FAMutConvJob(src, dst, f, cond, offset, 0, src.size - 1, FAJob.threshold(src.size), dst)

}
