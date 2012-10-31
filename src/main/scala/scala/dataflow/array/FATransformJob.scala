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
    this(src, dst, f, 0, src.size - 1,
         scala.collection.parallel.thresholdFromSize(
           src.size, scala.collection.parallel.availableProcessors
         ), dst)

  protected def subJobs: (FAJob, FAJob) = {
    val ((s1, e1), (s2, e2)) = splitInds
    
    (new FATransformJob(src, dst, f, s1, e1, thresh, this),
     new FATransformJob(src, dst, f, s2, e2, thresh, this))
  }

  protected def doCompute() {
    for (i <- start to end) {
      dst.data(i) = f(src.data(i))
    }
  }

}
