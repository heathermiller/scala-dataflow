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

  protected final def dispatch(newJob: FAJob, srcOffset: Int, length: Int) {
    val curJob = /*READ*/srcJob

    if (curJob == null) {
        FAJob.schedule(newJob)
    } else if (srcOffset == 0 && length == size) {
      curJob.depending(newJob)
    } else {
      // We need to realign the thing... :(
      val raj = FAAlignJob(this, srcOffset, length - 1 + srcOffset)
      FAJob.schedule(raj)
      raj.depending(newJob)
    }
  }

  private[array] final def tryAddObserver(obs: FAJob.Observer) = {
    val curJob = /*READ*/srcJob
    curJob != null && curJob.tryAddObserver(obs)
  }

  def done = {
    val job = /*READ*/srcJob
    job == null || job.done
  }

  final protected def setDone() { srcJob = null }

}
