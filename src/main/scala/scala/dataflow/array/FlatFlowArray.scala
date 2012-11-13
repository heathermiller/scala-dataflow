package scala.dataflow.array

import scala.dataflow.Future
import scala.annotation.tailrec

class FlatFlowArray[A : ClassManifest](
  private[array] val data: Array[A]
) extends FlowArray[A] with FAJob.Observer {

  import FlowArray._

  // Fields
  val size = data.length

  // Calculation Information
  @volatile private[array] var srcJob: FAJob = null

  // Helpers
  @inline private final def dispatch[B : ClassManifest](
    newJob: FAJob,
    dest: FlatFlowArray[B]
  ) {
    // Setup destination
    dest.srcJob = newJob

    dispatch(newJob)
  }

  @inline private[array] final def dispatch(newJob: FAJob) {
    val curJob = /*READ*/srcJob

    // Schedule job
    if (curJob != null)
      curJob.depending(newJob)
    else
      FAJob.schedule(newJob)
  }

  def newFA[B : ClassManifest] = 
    new FlatFlowArray(new Array[B](length))

  def newFA[B : ClassManifest](fact: Int) = 
    new FlatFlowArray(new Array[B](length * fact))

  // Functions
  def map[B : ClassManifest](f: A => B): FlowArray[B] = {
    val ret = newFA[B]
    dispatch( FAMapJob(this, ret, f), ret )
    ret
  }

  def flatMapN[B : ClassManifest](n: Int)(f: A => FlowArray[B]): FlowArray[B] = {
    val ret = newFA[B](n)
    dispatch( FAFlatMapJob(this, ret, f.asInstanceOf[A=>FlatFlowArray[B]], n), ret )
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

  def fold[A1 >: A](z: A1)(op: (A1, A1) => A1): Future[A1] = {
    import FAFoldJob.FoldFuture

    val job = FAFoldJob(this, z, op)
    val fut = new FoldFuture(job)
    job.addObserver(fut)

    dispatch(job)
    fut
  }

  override def jobDone() {
    srcJob = null
    freeBlocked()
  }

  override def blocking = {
    block()
    data
  }

  /**
   * Checks if this job is done
   *
   * This may NOT be implemented by checking waiting == Complete because otherwise
   * the jobs that are woken up by jobDone will park again!
   */
  def done = /*READ*/srcJob == null

}
