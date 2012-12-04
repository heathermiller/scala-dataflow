package scala.dataflow.array

private[array] class FAFoldConsolidateJob[A : ClassManifest] private (
  val src: FlatFlowArray[FoldFuture[A]],
  start: Int,
  end: Int,
  thr: Int,
  obs: FAJob.Observer
) extends FAJob(start, end, thr, obs) {

  override protected type SubJob = FAFoldConsolidateJob[A]

  protected def subCopy(s: Int, e: Int) = 
    new FAFoldConsolidateJob(src, s, e, thresh, this)

  protected def doCompute() {
    val jobs = for {
      ff <- src.data.slice(start,end)
       j <- ff.getJob
    } yield j

    delegate(jobs)
  }

}

object FAFoldConsolidateJob {

  import FAJob.JobGen

  def apply[A : ClassManifest](src: FlatFlowArray[FoldFuture[A]], srcOffset: Int, length: Int) = {
    new FAFoldConsolidateJob(src, srcOffset, srcOffset + length - 1,
                             FAJob.threshold(length), null)
  }

}
