package scala.dataflow.array

import scala.dataflow.Future
import scala.annotation.tailrec

class FlowArray[A : ClassManifest](
  private[array] val data: Array[A]
) extends FAJob.Observer {

  import FlowArray._

  // Fields
  val size = data.length
  def length = size

  // Calculation Information
  @volatile private var srcJob: FAJob = null
  @volatile private var waiting: WaitList = Empty

  // Unsafe stuff
  private val unsafe = getUnsafe()
  private val OFFSET = unsafe.objectFieldOffset(classOf[FlowArray[_]].getDeclaredField("waiting"))
  @inline private def CAS(ov: WaitList, nv: WaitList) =
    unsafe.compareAndSwapObject(this, OFFSET, ov, nv)

  // Helpers
  @inline private final def dispatch[B : ClassManifest](
    newJob: FAJob,
    dest: FlowArray[B]
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
    new FlowArray(new Array[B](length))

  def newFA[B : ClassManifest](fact: Int) = 
    new FlowArray(new Array[B](length * fact))

  // Functions
  def map[B : ClassManifest](f: A => B): FlowArray[B] = {
    val ret = newFA[B]
    dispatch( FAMapJob(this, ret, f), ret )
    ret
  }

  def flatMapN[B : ClassManifest](n: Int)(f: A => FlowArray[B]): FlowArray[B] = {
    val ret = newFA[B](n)
    dispatch( FAFlatMapJob(this, ret, f, n), ret )
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

    @tailrec
    def done0: Unit = /*READ*/waiting match {
      case Empty => 
        if (!CAS(Empty, Complete)) done0
      case ov@Blocking(thr, next) =>
        if (CAS(ov, next)) {
          unsafe.unpark(thr)
          done0
        } else done0
      case Complete =>
        /* this can sporadically happen, as jobDone needs to be idempotent */
    }

    done0

  }

  /**
   * Checks if this job is done
   *
   * This may NOT be implemented by checking waiting == Complete because otherwise
   * the jobs that are woken up by jobDone will park again!
   */
  def done = /*READ*/srcJob == null

  @tailrec
  final def blocking: Array[A] = {
    val curo = /*READ*/waiting

    if (done || curo == Complete) data
    else {
      val nv = Blocking(Thread.currentThread, curo)
      if (CAS(curo, nv))
        unsafe.park(false, 0)

      blocking
    }
  }

}

object FlowArray {

  sealed abstract class WaitList
  case object Empty    extends WaitList
  case object Complete extends WaitList
  case class  Blocking(thr: Thread, next: WaitList) extends WaitList

  def tabulate[A : ClassManifest](n: Int)(f: Int => A) = {
    val ret = new FlowArray(new Array[A](n))
    val job = FAGenerateJob(ret, f, ret)
    ret.srcJob = job
    FAJob.schedule(job)
    ret
  }

}
