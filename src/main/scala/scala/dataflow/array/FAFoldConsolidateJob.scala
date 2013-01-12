package scala.dataflow.array

/**
 * waits on all FoldFutures in a FlowArray[FoldFuture[A]] only then
 * continues in dependency chain
 */
private[array] class FAFoldConsolidateJob[A] private (
  val src: FlowArray[FoldFuture[A]],
  start: Int,
  end: Int,
  thr: Int,
  obs: FAJob.Observer
) extends FAJob(start, end, thr, obs) {

  override protected type SubJob = FAFoldConsolidateJob[A]

  override protected def subCopy(s: Int, e: Int) = 
    new FAFoldConsolidateJob(src, s, e, thresh, this)

  override protected def doCompute() {
    val jobs = for {
      i <- start to end
      j <- src.unsafe(i).getJob
    } yield j

    if (!jobs.isEmpty) {
      delegate(jobs)
    }
  }

}

private[array] object FAFoldConsolidateJob {

  import FAJob.JobGen

  /** creates a new FAFoldConsolidateJob */
  def apply[A](src: FlowArray[FoldFuture[A]], srcOffset: Int, length: Int) = {
    new FAFoldConsolidateJob(src, srcOffset, srcOffset + length - 1,
                             FAJob.threshold(length), null)
  }

}
