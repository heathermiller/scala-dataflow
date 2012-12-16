package scala.dataflow.array

import scala.dataflow.Future
import scala.annotation.tailrec

abstract class ConcreteFlowArray[A : ClassManifest] extends FlowArray[A] with FAJob.Observer {

  import FlowArray._
  import SlicedJob._

  // Calculation Information
  @volatile private var srcJob: FAJob = null

  override def slice(start: Int, end: Int): FlowArray[A] =
    new FlowArraySliceView(this, start, end - start + 1)

  private[array] def transpose(from: Int, to: Int)(step: Int) = {
    val len = to - from + 1
    val ret = new FlatFlowArray(new Array[A](len))

    val tjob = dispatch(FATransposeJob(ret, step, from, len), 0, from, len)

    val ajob = FAAlignJob(tjob, 0, len)
    FAJob.schedule(ajob)

    ret.generatedBy(ajob)
    ret
  }

  /** returns a job that aligns on this FlowArray with given offset and size */
  private[array] def align(offset: Int, size: Int) =
    FAAlignJob(this, offset, size - 1 + offset)

  private[array] final def generatedBy(fa: ConcreteFlowArray[_]) {
    val curJob = /*READ*/fa.srcJob
    if (curJob != null)
      generatedBy(curJob)
  }

  private[array] final def generatedBy(job: FAJob) {
    srcJob = job
    job.addObserver(this)
  }

  // Slice-wise dependencies
  private[array] override def sliceJobs(from: Int, to: Int): SliceDep = {
    for {  j <- Option(/*READ*/srcJob)
          js <- Some(j.destSliceJobs(from, to).filterNot(_.done)) if !js.isEmpty
        } yield (js, false)
  }

  private[array] final def dispatch(newJob: FAJob, srcOffset: Int, length: Int) {
    val curJob = /*READ*/srcJob

    if (curJob == null) {
      FAJob.schedule(newJob)
    } else if (srcOffset == 0 && length == size) {
      curJob.depending(newJob)
    } else {
      // We need to realign the thing... :(
      val raj = this.align(srcOffset, length)
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
