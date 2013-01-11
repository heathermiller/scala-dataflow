package scala.dataflow.array

import scala.dataflow.Blocker
import scala.dataflow.Future
import scala.annotation.tailrec
import scala.reflect.ClassTag

/**
 * Abstract base class for all FlowArrays.
 *
 * Defines most monadic operations through the abstract dispatch
 * method.
 */
abstract class FlowArray[A : ClassTag] extends Blocker with FAJob.Observer with SlicedJob { 

  import FlowArray._
  import SlicedJob._

  /** alias for JobGens that can be dispatched by this FA */
  protected type JobGen = FAJob.JobGen[A]

  ///// Public API /////

  /** the number of elements in this FA */
  def size: Int
  /** alias for size */
  final def length = size

  /** apply a function to each element */
  def map[B : ClassTag](f: A => B): FlowArray[B] =
    mapToFFA(f)

  /**
   * zip two FlowArrays together
   * 
   * zips two FlowArrays. Both FAs need to have the same size
   */
  def zip[B : ClassTag](that: FlowArray[B]) =
    zipMap(that)((_,_))

  /**
   * zips then maps at once
   * 
   * equivalent (in behavior, not speed) to:
   * `this.zip(that).map(f.tupled)`
   */
  def zipMap[B : ClassTag, C : ClassTag](that: FlowArray[B])
                                        (f: (A,B) => C): FlowArray[C] = {
      
    require(size == that.size)
    val ret = newFA[C]
    setupDep(FAZipMapJob(that, ret, f), ret)
  }

  /**
   * flatMap with fixed inner size
   *
   * @param n size of each FlowArray returned by f
   * @param f maps an element to a FlowArray
   */
  def flatMapN[B : ClassTag](n: Int)(f: A => FlowArray[B]): FlowArray[B] = {
    val ret = newFA[B](n)
    setupDep(FAFlatMapJob(ret, f, n), ret)
  }

  /**
   * mutably converge to a value
   *
   * applies `it` to each element until `cond` is met. Returns new value
   * @param cond condition of convergance
   * @param it iteration step
   */
  def mutConverge(cond: A => Boolean)(it: A => Unit): FlowArray[A] = {
    val ret = newFA[A]
    setupDep(FAMutConvJob(ret, it, cond), ret)
  }

  /**
   * converge to a value
   *
   * calculates repeatedly the result of `it` applied to a given
   * element until `cond` is met.
   * @param cond condition of convergance
   * @param it iteration step
   */
  def converge(cond: A => Boolean)(it: A => A): FlowArray[A] = {
    val ret = newFA[A]
    setupDep(FAIMutConvJob(ret, it, cond), ret)
  }

  /**
   * out of order folding
   *
   * folds this FA using `op` and `z` as neutral element.
   * @param z neutral element
   * @param op reduction function. must be associative
   */
  final def fold[A1 >: A](z: A1)(op: (A1, A1) => A1): FoldFuture[A1] =
    fold[A1](0, size - 1)(z)(op)

  /** take a slice of this FA */
  def slice(start: Int, end: Int): FlowArray[A]

  /**
   * partitions this FA into n chunks
   *
   * requires the size of this FA to be divisible by the number of
   * chunks
   * @param n number of chunks (size % n = 0)
   */
  def partition(n: Int): FlowArray[FlowArray[A]] = tabulate(n) { x =>
    slice(x * size / n, (x + 1) * size / n)
  }

  /**
   * flatten this FA
   *
   * requires the size of the inner FlowArrays / Containers. Use `n = 1`
   * to flatten FoldFutures
   * @param n size of inner containers
   */
  def flatten[B](n: Int)(implicit flat: CanFlatten[A,B],
                         mf: ClassTag[B]): FlowArray[B]

  /**
   * transpose this FA
   *
   * interprets this FA as a 2-dimensional matrix with one dimension
   * equal to `dim` and transposes it. Requires the size of this FA to
   * be divisible by `dim`
   * @param dim inner dimension for transpose (size % dim = 0)
   */
  def transpose(dim: Int): FlowArray[A] = transpose(0, size - 1)(dim)

  /**
   * zips, maps and folds in one shot.
   *
   * equivalent (in behavior) to:
   * `this.zip(that).map(f.tupled).fold(z)(op)`
   */
  final def zipMapFold[B : ClassTag, C](that: FlowArray[B])
                                      (f: (A,B) => C)
                                      (z: C)
                                      (op: (C,C) => C): FoldFuture[C] =
    zipMapFold(0, size - 1)(that)(f)(z)(op)


  /** Checks if this job is done */
  def done: Boolean

  /**
   * wait for completion of this FA
   *
   * Suspends the calling thread until this FA is completed or a
   * timeout occurs. If it succeeds, returns an array with the
   * calculated elements.
   *
   * the semantics of `isAbs` and `msecs` is the same as for
   * `sum.misc.Unsafe.park`
   */
  def blocking(isAbs: Boolean, msecs: Long): Array[A]

  /**
   * wait for completion of this FA
   *
   * Suspends the calling thread until this FA is completed. Returns
   * an array with the elements
   */
  final def blocking: Array[A] = blocking(false, 0)

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

  private[array] def mapToFFA[B : ClassTag](f: A => B): FlatFlowArray[B] = {
    val ret = newFA[B]
    setupDep(FAMapJob(ret, f), ret)
    ret
  }

  private[array] def fold[A1 >: A](from: Int, to: Int)(z: A1)(op: (A1,
  A1) => A1): FoldFuture[A1]

  private[array] def transpose(from: Int, to: Int)(step: Int):
  FlowArray[A]

  private[array] def zipMapFold[B : ClassTag, C](from: Int, to: Int)
                                      (that: FlowArray[B])
                                      (f: (A,B) => C)
                                      (z: C)
                                      (op: (C,C) => C): FoldFuture[C]


  ///// Private utility functions /////

  @inline private final def newFA[B : ClassTag] = 
    new FlatFlowArray(new Array[B](length))

  @inline private final def newFA[B : ClassTag](n: Int) = 
    new HierFlowArray(new Array[FlowArray[B]](size), n)

  @inline private final def setupDep[B](gen: JobGen, ret: ConcreteFlowArray[B]) = {
    val job = dispatch(gen)
    ret.generatedBy(job)
    ret
  }

}

object FlowArray {

  def tabulate[A : ClassTag](n: Int)(f: Int => A): FlowArray[A] = {
    val ret = new FlatFlowArray(new Array[A](n))
    val job = FAGenerateJob(ret, f)
    ret.generatedBy(job)
    FAJob.schedule(job)
    ret
  }

  def apply[A : ClassTag](xs: A*) = new FlatFlowArray(xs.toArray)

  def setPar(n: Int) = FAJob.setPar(n)

}
