package scala.dataflow.array

import scala.dataflow.Future
import scala.annotation.tailrec

abstract class FlowArray[A : ClassManifest] extends FAJob.Observer {

  import FlowArray._

  type JobGen = (FlatFlowArray[A], Int) => FAJob

  // Fields
  def size: Int
  def length = size

  // Calculation Information
  @volatile private var srcJob: FAJob = null

  // Utilities
  @inline protected final def newFA[B : ClassManifest] = 
    new FlatFlowArray(new Array[B](length))

  @inline protected final def newFA[B : ClassManifest](n: Int) = 
    new HierFlowArray(new Array[FlowArray[B]](size), n)

  private[array] def copyToArray(trg: Array[A], offset: Int): Unit

  // Slice-wise dependencies
  private[array] def sliceJobs(from: Int, to: Int): SliceDep = {
    for { j  <- Option(/*READ*/srcJob)
          sj <- Some(j.destSliceJob(from, to)) if !sj.done
        } yield (Vector(sj), false)
  }

  // Dispatcher
  private[array] def dispatch(gen: JobGen): FAJob = dispatch(gen, 0)
  private[array] def dispatch(gen: JobGen, offset: Int): FAJob

  protected final def dispatch(newJob: FAJob) {
    val curJob = /*READ*/srcJob

    // Schedule job
    if (curJob != null)
      curJob.depending(newJob)
    else
      FAJob.schedule(newJob)
  }

  @inline private final def setupDep[B](gen: JobGen, ret: FlowArray[B]) = {
    val job = dispatch(gen)
    ret.srcJob = job
    job.addObserver(ret)
    ret
  }

  // Public members
  def map[B : ClassManifest](f: A => B): FlowArray[B] = {
    val ret = newFA[B]
    setupDep((fa, of) => FAMapJob(fa, ret, f, of), ret)
  }

  def zip[B : ClassManifest](that: FlowArray[B]) = zipMap(that)((_,_))

  def zipMap[B : ClassManifest, C : ClassManifest](
    that: FlowArray[B])(f: (A,B) => C): FlowArray[C] = {
      
    require(size == that.size)
    val ret = newFA[C]
    setupDep((fa, of) => FAZipMapJob(fa, that, ret, f, of), ret)
  }

  def flatMapN[B : ClassManifest](n: Int)(f: A => FlowArray[B]): FlowArray[B] = {
    val ret = newFA[B](n)
    setupDep((fa, of) => FAFlatMapJob(fa, ret, f, n, of), ret)
  }

  def mutConverge(cond: A => Boolean)(it: A => Unit): FlowArray[A] = {
    val ret = newFA[A]
    setupDep((fa, of) => FAMutConvJob(fa, ret, it, cond, of), ret)
  }

  def converge(cond: A => Boolean)(it: A => A): FlowArray[A] = {
    val ret = newFA[A]
    setupDep((fa, of) => FAIMutConvJob(fa, ret, it, cond, of), ret)
  }

  def fold[A1 >: A](z: A1)(op: (A1, A1) => A1): Future[A1] = {
    val ret = new FoldFuture(z, op)
    val job = dispatch((fa, of) => FAFoldJob(fa, ret, z, op))
    job.addObserver(ret)
    ret
  }

  private[array] final def tryAddObserver(obs: FAJob.Observer) = {
    val curJob = /*READ*/srcJob
    curJob != null && curJob.tryAddObserver(obs)
  }

  private[array] final def addObserver(obs: FAJob.Observer) {
    if (!tryAddObserver(obs)) obs.jobDone()
  }

  /**
   * Checks if this job is done
   *
   * This may NOT be implemented by checking waiting == Complete because otherwise
   * the jobs that are woken up by jobDone will park again!
   */
  def done = {
    val job = /*READ*/srcJob
    job == null || job.done
  }

  def unsafe(i: Int): A
  def blocking(isAbs: Boolean, msecs: Long): Array[A]
  def blocking: Array[A] = blocking(false, 0)

  final protected def setDone() { srcJob = null }

}

object FlowArray {

  type SliceDep = Option[(IndexedSeq[FAJob], Boolean)]

  def tabulate[A : ClassManifest](n: Int)(f: Int => A) = {
    val ret = new FlatFlowArray(new Array[A](n))
    val job = FAGenerateJob(ret, f)
    ret.srcJob = job
    FAJob.schedule(job)
    ret
  }

}
