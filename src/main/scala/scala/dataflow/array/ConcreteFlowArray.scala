package scala.dataflow.array

import scala.dataflow.Future
import scala.annotation.tailrec

abstract class ConcreteFlowArray[A : ClassManifest] extends FlowArray[A] with FAJob.Observer {

  import FlowArray._

  // Calculation Information
  @volatile private var srcJob: FAJob = null

  private[array] final def generatedBy(job: FAJob) {
    srcJob = job
    job.addObserver(this)
  }

  // Slice-wise dependencies
  private[array] override def sliceJobs(from: Int, to: Int): SliceDep = {
    for { j  <- Option(/*READ*/srcJob)
          sj <- Some(j.destSliceJob(from, to)) if !sj.done
        } yield (Vector(sj), false)
  }

  protected final def dispatch(newJob: FAJob) {
    val curJob = /*READ*/srcJob

    // Schedule job
    if (curJob != null)
      curJob.depending(newJob)
    else
      FAJob.schedule(newJob)
  }

  private[array] final def tryAddObserver(obs: FAJob.Observer) = {
    val curJob = /*READ*/srcJob
    curJob != null && curJob.tryAddObserver(obs)
  }

  private[array] final def addObserver(obs: FAJob.Observer) {
    if (!tryAddObserver(obs)) obs.jobDone()
  }

  def done = {
    val job = /*READ*/srcJob
    job == null || job.done
  }

  final protected def setDone() { srcJob = null }

}
