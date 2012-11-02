package scala.dataflow.array

import scala.dataflow.Future
import scala.annotation.tailrec

class FlowArray[A : ClassManifest](
  private[array] val data: Array[A]
) extends FAJob.Observer {

  // Fields
  val size = data.length
  def length = size

  // Calculation Information
  @volatile private var srcJob: FAJob = null

  // Functions
  def map[B : ClassManifest](f: A => B): FlowArray[B] = {
    val ret = new FlowArray(new Array[B](length))
    val newJob = FATransformJob(this, ret, f)
    val curJob = /*READ*/srcJob

    // Setup destination
    ret.srcJob = newJob

    // Schedule job
    if (curJob != null)
      curJob.depending(newJob)
    else
      FAJob.schedule(newJob)

    ret
  }

  /*
  def converge(cond: A => Boolean)(it: A => A) =
    dispatchTransJob(convJob(cond, it) _)

  def converge(count: Int)(it: A => A) =
    dispatchTransJob(convJob(count, it) _)
    */

  def fold[A1 >: A](z: A1)(op: (A1, A1) => A1): Future[A1] = {
    val (job, fut) = FAFoldJob(this, z, op)
    val curJob = /*READ*/srcJob

    if (curJob != null)
      curJob.depending(job)
    else
      FAJob.schedule(job)

    fut
  }

  override def jobDone() {
    synchronized { notifyAll() }
  }

  def done = {
    val curJob = /*READ*/srcJob
    if (curJob == null)
      true
    else {
      if (curJob.done) {
        srcJob/*WRITE*/ = null
        true
      } else false
    }
  }

  def blocking = {
    synchronized {
      while (!done) wait()
    }
    data
  }
  

}
