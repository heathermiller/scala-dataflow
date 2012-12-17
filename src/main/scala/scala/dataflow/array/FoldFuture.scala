package scala.dataflow.array

import scala.dataflow.Future

class FoldFuture[A](j: FAResultJob[A]) extends Future[A] with FAJob.Observer {
  
  j.addObserver(this)

  @volatile
  private var job: FAResultJob[A] = j

  override def getOption = {
    Option(job).map(_.getResult) orElse
    super.getOption
  }

  final def getJob: Option[FAJob] = Option(job)

  final def setJob(j: FAResultJob[A]) { job = j }

  override def jobDone() {
    val curj = job
    // TODO, why is curj.done required here?
    if (curj != null && curj.done) {
      tryComplete(curj.getResult)
      job = null
    }
  }

}
