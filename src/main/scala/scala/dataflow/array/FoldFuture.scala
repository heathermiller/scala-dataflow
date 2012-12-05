package scala.dataflow.array

import scala.dataflow.Future

class FoldFuture[A] extends Future[A] with FAJob.Observer {
  
  @volatile private var job: FAFoldJob[_,A] = null

  override def getOption = {
    Option(job).map(_.getResult) orElse
    super.getOption
  }

  final def getJob: Option[FAJob] = Option(job)

  final def setJob(j: FAFoldJob[_,A]) { job = j }

  override def jobDone() {
    if (job != null)
      tryComplete(job.getResult)
    job = null
  }

}
