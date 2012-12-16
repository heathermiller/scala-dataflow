package scala.dataflow.array

import scala.dataflow.Blocker
import scala.dataflow.Future
import scala.annotation.tailrec

abstract class FlowArray[A : ClassManifest] extends Blocker with FAJob.Observer with SlicedJob {

  import FlowArray._

  type JobGen = FAJob.JobGen[A]

  ///// Public API /////

  def size: Int
  def length = size

  def map[B : ClassManifest](f: A => B): FlowArray[B] = mapToFFA(f)

  def zip[B : ClassManifest](that: FlowArray[B]) = zipMap(that)((_,_))

  def zipMap[B : ClassManifest, C : ClassManifest](
    that: FlowArray[B])(f: (A,B) => C): FlowArray[C] = {
      
    require(size == that.size)
    val ret = newFA[C]
    setupDep(FAZipMapJob(that, ret, f), ret)
  }

  def flatMapN[B : ClassManifest](n: Int)(f: A => FlowArray[B]): FlowArray[B] = {
    val ret = newFA[B](n)
    setupDep(FAFlatMapJob(ret, f, n), ret)
  }

  def mutConverge(cond: A => Boolean)(it: A => Unit): FlowArray[A] = {
    val ret = newFA[A]
    setupDep(FAMutConvJob(ret, it, cond), ret)
  }

  def converge(cond: A => Boolean)(it: A => A): FlowArray[A] = {
    val ret = newFA[A]
    setupDep(FAIMutConvJob(ret, it, cond), ret)
  }

  def fold[A1 >: A](from: Int, to: Int)(z: A1)(op: (A1, A1) => A1): FoldFuture[A1]

  def fold[A1 >: A](z: A1)(op: (A1, A1) => A1): FoldFuture[A1] =
    fold[A1](0, size - 1)(z)(op)

  def slice(start: Int, end: Int): FlowArray[A]

  /** partitions this FA into n chunks */
  def partition(n: Int): FlowArray[FlowArray[A]] = tabulate(n) { x =>
    slice(x * size / n, (x + 1) * size / n - 1)
  }

  def flatten[B](n: Int)(implicit flat: CanFlatten[A,B], mf: ClassManifest[B]): FlowArray[B]

  def transpose(step: Int): FlowArray[A] = transpose(0, size - 1)(step)
  def transpose(from: Int, to: Int)(step: Int): FlowArray[A]

  /** Checks if this job is done */
  def done: Boolean

  def blocking(isAbs: Boolean, msecs: Long): Array[A]

  def blocking: Array[A] = blocking(false, 0)

  ///// FlowArray package private API /////

  private[array] def unsafe(i: Int): A

  private[array] def copyToArray(dst: Array[A], srcPos: Int, dstPos: Int, length: Int): Unit

  // Slice-wise dependencies
  private[array] def sliceJobs(from: Int, to: Int): SliceDep

  // Dispatcher
  private[array] def dispatch(gen: JobGen): FAJob = dispatch(gen, 0, 0, size)
  private[array] def dispatch(gen: JobGen, dstOffset: Int, srcOffset: Int, length: Int): FAJob

  private[array] def tryAddObserver(obs: FAJob.Observer): Boolean

  private[array] final def addObserver(obs: FAJob.Observer) {
    if (!tryAddObserver(obs)) obs.jobDone()
  }

  private[array] def mapToFFA[B : ClassManifest](f: A => B): FlatFlowArray[B] = {
    val ret = newFA[B]
    setupDep(FAMapJob(ret, f), ret)
    ret
  }

  ///// Private utility functions /////

  @inline private final def newFA[B : ClassManifest] = 
    new FlatFlowArray(new Array[B](length))

  @inline private final def newFA[B : ClassManifest](n: Int) = 
    new HierFlowArray(new Array[FlowArray[B]](size), n)

  @inline private final def setupDep[B](gen: JobGen, ret: ConcreteFlowArray[B]) = {
    val job = dispatch(gen)
    ret.generatedBy(job)
    ret
  }

}

object FlowArray {

  def tabulate[A : ClassManifest](n: Int)(f: Int => A): FlowArray[A] = {
    val ret = new FlatFlowArray(new Array[A](n))
    val job = FAGenerateJob(ret, f)
    ret.generatedBy(job)
    FAJob.schedule(job)
    ret
  }

  def apply[A : ClassManifest](xs: A*) = new FlatFlowArray(xs.toArray)

}
