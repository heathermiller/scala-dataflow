package scala.dataflow.array

private[array] class FAFoldJob[A : ClassManifest, A1 >: A] private (
  val src: FlowArray[A],
  val z: A1,
  val f: (A1, A1) => A1,
  start: Int,
  end: Int,
  thr: Int,
  obs: FAJob.Observer
) extends FAJob(start, end, thr, obs) {

  var result: A1 = z

  def this(src: FlowArray[A], z: A1, f: (A1, A1) => A1, obs: FAJob.Observer) =
    this(src, z, f, 0, src.size - 1, FAJob.threshold(src.size), obs)

  protected def subCopy(s: Int, e: Int) = 
    new FAFoldJob(src, z, f, s, e, thresh, this)

  override def jobDone() {
    if (done) {
      val (j1, j2) = subTasks
      result = f(j1.asInstanceOf[FAFoldJob[A,A1]].result,
                 j2.asInstanceOf[FAFoldJob[A,A1]].result)
      notifyObservers()
    }
  }

  protected def doCompute() {
    var tmp = z
    for (i <- start to end) {
      tmp = f(tmp, src.data(i))
    }
    result = tmp
  }

}

object FAFoldJob {

  import scala.dataflow.Future

  def apply[A : ClassManifest, A1 >: A](src: FlowArray[A], z: A1, f: (A1, A1) => A1) = {
    val job = new FAFoldJob(src, z, f, null)
    val fut = new FoldFuture(job)
    job.observer = fut
    (job, fut)
  }

  class FoldFuture[A](job: FAFoldJob[_,A]) extends Future[A] with FAJob.Observer {
    override def jobDone() { complete(job.result) }
  }

}
