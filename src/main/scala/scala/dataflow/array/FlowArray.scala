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
   * `sun.misc.Unsafe.park`
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

  /**
   * access an element in this FA
   * 
   * returns the element at index `i` in this FA. *Does not check for
   * completion* of this FA
   */
  private[array] def unsafe(i: Int): A

  /**
   * copy elements from this FA
   *
   * copy a slice of this FA into an array. *Does not check for
   * completion* of this FA
   * @param dst array to copy to
   * @param srcPos position in this FA to start copying
   * @param dstPos position in this FA to store first element
   * @param length number of elements to copy
   */
  private[array] def copyToArray(dst: Array[A], srcPos: Int, dstPos: Int, length: Int): Unit

  /**
   * slice-wise dependencies
   *
   * calculates the jobs responsible for the given slice. See
   * `SliceDep` for documentation of the return type.
   */
  private[array] def sliceJobs(from: Int, to: Int): SliceDep

  /**
   * dispatch a job (or multiple ones) on this FA
   *
   * Calls JobGen an adequate number of times to generate jobs for
   * this whole FA. Then schedules all these jobs and return the job
   * responsible for dependency tracking of the all the jobs
   */
  private[array] final def dispatch(gen: JobGen): FAJob =
    dispatch(gen, 0, 0, size)

  /**
   * dispatch a job (or multiple ones) on this FA
   *
   * Calls JobGen an adequate number of times to generate jobs for the 
   * given slice of this FA. Then schedules all these jobs and return
   * the job responsible for dependency tracking of the all the jobs
   */
  private[array] def dispatch(gen: JobGen, dstOffset: Int, srcOffset: Int, length: Int): FAJob

  /**
   * Add observer to this FA.
   *
   * Add an observer to this FA which is notified once it completes or
   * return false, if this FA is already completed.
   *
   * This method is required to avoid unnecessary stack growth, where
   * tail recursion could have been used
   * 
   * @param obs Observer to add
   * @return true if the observer could be added, false if this FA is
   * completed
   */
  private[array] def tryAddObserver(obs: FAJob.Observer): Boolean

  /**
   * Add observer to this FA
   *
   * Add an observer which is notified once this FA completes
   */
  private[array] final def addObserver(obs: FAJob.Observer) {
    if (!tryAddObserver(obs)) obs.jobDone()
  }

  /**
   * the same as `map`, but returns a qualified FlatFlowArray instead
   * of a FlowArray only. This is purely here for nice typing.
   */
  private[array] def mapToFFA[B : ClassTag](f: A => B): FlatFlowArray[B] = {
    val ret = newFA[B]
    setupDep(FAMapJob(ret, f), ret)
    ret
  }

  /** folds a slice of this FA. Same rules as for normal fold */
  private[array] def fold[A1 >: A](from: Int, to: Int)
                                  (z: A1)
                                  (op: (A1, A1) => A1): FoldFuture[A1]

  /** transpose a slice of this FA. Same rules as for normal transpose */
  private[array] def transpose(from: Int, to: Int)(step: Int): FlowArray[A]

  /**
   * zipMapFold a slice of this FA. Same rules as for normal
   * zipMapFold hold
   */
  private[array] def zipMapFold[B : ClassTag, C](from: Int, to: Int)
                                      (that: FlowArray[B])
                                      (f: (A,B) => C)
                                      (z: C)
                                      (op: (C,C) => C): FoldFuture[C]


  ///// Private utility functions /////

  /** create new FA with same size as this one */
  @inline
  private final def newFA[B : ClassTag] = 
    new FlatFlowArray(new Array[B](length))

  /**
   * create new HFA having the same number of inner FAs as the size of
   * this FA.
   * @param n size of inner FAs
   */
  @inline
  private final def newFA[B : ClassTag](n: Int) = 
    new HierFlowArray(new Array[FlowArray[B]](size), n)

  /**
   * setup dependencies between a CFA and a JobGen that generates it
   *
   * dispatches the JobGen on this FA and sets the resulting job as
   * generator to `ret`.
   * @return ret
   */
  @inline
  private final def setupDep[B](gen: JobGen, ret: ConcreteFlowArray[B]) = {
    val job = dispatch(gen)
    ret.generatedBy(job)
    ret
  }

}

object FlowArray {

  /**
   * create a FA of size `n`
   *
   * creates an FA of size `n` by calling `f` with the index of each
   * element to be created.
   */
  def tabulate[A : ClassTag](n: Int)(f: Int => A): FlowArray[A] = {
    val ret = new FlatFlowArray(new Array[A](n))
    val job = FAGenerateJob(ret, f)
    ret.generatedBy(job)
    FAJob.schedule(job)
    ret
  }

  /** create a FA with the given elements */
  def apply[A : ClassTag](xs: A*) = new FlatFlowArray(xs.toArray)

  /**
   * set parallelism level of the FA implementation.
   *
   * Needs to be called before any other operation in order to work.
   */
  def setPar(n: Int) = FAJob.setPar(n)

}
