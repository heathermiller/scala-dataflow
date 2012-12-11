package scala.dataflow.array

private[array] class FATransposeConsolidateJob private (
  val src: FATransposeJob[_],
  start: Int,
  end: Int,
  thr: Int,
  obs: FAJob.Observer
) extends FAJob(start, end, thr, obs) {

  override protected type SubJob = FATransposeConsolidateJob

  protected def subCopy(s: Int, e: Int) = 
    new FATransposeConsolidateJob(src, s, e, thresh, this)

  protected def doCompute() {
    val jobs = src.destSliceJobs(start, end)

    if (!jobs.isEmpty) {
      delegate(jobs)
    }
  }

}

object FATransposeConsolidateJob {

  def apply[A](src: FATransposeJob[_]) = {
    val len = src.end - src.start + 1

    new FATransposeConsolidateJob(src, 0, len - 1, FAJob.threshold(len), null)
  }

}
