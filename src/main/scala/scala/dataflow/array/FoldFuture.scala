package scala.dataflow.array

import scala.dataflow.Future

class FoldFuture[A] extends Future[A] with FAJob.Observer {
  
  @volatile private var job: FAFoldJob[_,A] = null

  final def getJob = Option(job)

  final def setJob(j: FAFoldJob[_,A]) { job = j }

  override def jobDone() {
    if (job != null)
      tryComplete(job.getResult)
    job = null
  }

}
