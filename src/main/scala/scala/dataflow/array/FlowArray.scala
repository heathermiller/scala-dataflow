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

  // Helpers
  @inline private def dispatch[B : ClassManifest](
    newJob: FAJob,
    dest: FlowArray[B]
  ) {
    // Setup destination
    dest.srcJob = newJob

    dispatch(newJob)
  }

  @inline private def dispatch(newJob: FAJob) {
    val curJob = /*READ*/srcJob

    // Schedule job
    if (curJob != null)
      curJob.depending(newJob)
    else
      FAJob.schedule(newJob)
  }

  def newFA[B : ClassManifest] = 
    new FlowArray(new Array[B](length))

  // Functions
  def map[B : ClassManifest](f: A => B): FlowArray[B] = {
    val ret = newFA[B]
    dispatch( FAMapJob(this, ret, f), ret )
    ret
  }

  def mutConverge(cond: A => Boolean)(it: A => Unit) = {
    val ret = newFA[A]
    dispatch( FAMutConvJob(this, ret, it, cond), ret )
    ret
  }

  def converge(cond: A => Boolean)(it: A => A) = {
    val ret = newFA[A]
    dispatch( FAIMutConvJob(this, ret, it, cond), ret )
    ret
  }

  /*
  def converge(count: Int)(it: A => A) =
    dispatchTransJob(convJob(count, it) _)
    */

  def fold[A1 >: A](z: A1)(op: (A1, A1) => A1): Future[A1] = {
    val (job, fut) = FAFoldJob(this, z, op)
    dispatch(job)
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
