package scala.dataflow.array

private[array] class FAAlignJob[A : ClassManifest] private (
  val src: ConcreteFlowArray[A],
  start: Int,
  end: Int,
  thr: Int,
  obs: FAJob.Observer
) extends FAJob(start, end, thr, obs) {

  protected def subCopy(s: Int, e: Int) =
    new FAAlignJob(src, s, e, thresh, this)

  protected def doCompute() {
    src.sliceJobs(start, end) match {
      // No need to retry after
      case Some((j, false)) => delegate(j)
      // Need to retry after
      case Some((j, true))  => delegateThen(j) { doCompute _ }
      // All done
      case None =>
    }
  }

}

object FAAlignJob {

  def apply[A : ClassManifest](
    src: ConcreteFlowArray[A],
    start: Int,
    end: Int
  ) = new FAAlignJob(src, start, end, FAJob.threshold(end - start + 1), null)

}
