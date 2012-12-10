package scala.dataflow.array

import scala.dataflow.Future

class FoldFuture[A](j: FAFoldJob[_,A]) extends Future[A] with FAJob.Observer {
  
  j.addObserver(this)

  @volatile private var job: FAFoldJob[_,A] = j

  override def getOption = {
    Option(job).map(_.getResult) orElse
    super.getOption
  }

  final def getJob: Option[FAJob] = Option(job)

  final def setJob(j: FAFoldJob[_,A]) { job = j }

  override def jobDone() {
    val curj = job
    // TODO, why is curj.done required here?
    if (curj != null && curj.done) {
      tryComplete(curj.getResult)
      job = null
    }
  }

}
