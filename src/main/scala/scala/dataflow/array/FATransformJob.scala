package scala.dataflow.array

private[array] class FATransformJob[A : ClassManifest, B : ClassManifest](
  val src: FlowArray[A],
  val dst: FlowArray[B],
  val f: A => B,
  start: Int,
  end: Int,
  obs: FAJob.Observer
) extends FAJob(start, end, obs) {

  protected def subJobs: (FAJob, FAJob) = {
    val ((s1, e1), (s2, e2)) = splitInds
    
    (new FATransformJob(src, dst, f, s1, e1, this),
     new FATransformJob(src, dst, f, s2, e2, this))
  }

  protected def doCompute() {
    for (i <- start to end) {
      dst.data(i) = f(src.data(i))
    }
  }

}
