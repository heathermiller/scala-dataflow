package scala.dataflow.array

private[array] class FAFlatMapJob[A : ClassManifest, B : ClassManifest] private (
  val src: FlatFlowArray[A],
  val dst: FlatFlowArray[B],
  val f: A => FlatFlowArray[B],
  val n: Int,
  start: Int,
  end: Int,
  thr: Int,
  obs: FAJob.Observer
) extends FAJob(start, end, thr, obs) {

  override protected val autoFinalize = false

  @volatile private var copies: Seq[FAJob] = null

  protected def subCopy(s: Int, e: Int) = 
    new FAFlatMapJob(src, dst, f, n, s, e, thresh, this)

  protected def doCompute() {
    copies = (start to end) map { i =>
      val sub = f(src.data(i))
      assert(sub.size == n)
      val copy = FACopyJob(sub, dst, i * n, this)
      sub.dispatch(copy)
      copy
    }
    // Test if already done!
    jobDone()
  }

  private def copiesDone =
    copies != null && copies.forall(_.done)

  override def jobDone() {
    if (copiesDone)
      // Bottom flatMap
      finalizeCompute()
    else if (done)
      // Split flatMap
      notifyObservers()
  }

}

object FAFlatMapJob {

  def apply[A : ClassManifest, B : ClassManifest](
    src: FlatFlowArray[A],
    dst: FlatFlowArray[B],
    f: A => FlatFlowArray[B],
    n: Int) =
      new FAFlatMapJob(src, dst, f, n, 0, src.size - 1, FAJob.threshold(src.size), dst)

}
