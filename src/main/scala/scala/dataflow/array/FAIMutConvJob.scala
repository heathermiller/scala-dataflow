package scala.dataflow.array

private[array] class FAIMutConvJob[A : ClassManifest] private (
  val src: FlowArray[A],
  val dst: FlowArray[A],
  val f: A => A,
  val cond: A => Boolean,
  start: Int,
  end: Int,
  thr: Int,
  obs: FAJob.Observer
) extends FAJob(start, end, thr, obs) {

  def this(src: FlowArray[A], dst: FlowArray[A], f: A => A, cond: A => Boolean) =
    this(src, dst, f, cond, 0, src.size - 1, FAJob.threshold(src.size), dst)

  protected def subCopy(s: Int, e: Int) = 
    new FAIMutConvJob(src, dst, f, cond, s, e, thresh, this)

  protected def doCompute() {
    for (i <- start to end) {
      var x = src.data(i)
      while (!cond(x)) {
        x = f(x)
      }
      dst.data(i) = x
    }
  }

}

object FAIMutConvJob {

  def apply[A : ClassManifest](
    src: FlowArray[A],
    dst: FlowArray[A],
    f: A => A,
    cond: A => Boolean
  ) = new FAIMutConvJob(src, dst, f, cond)

}
