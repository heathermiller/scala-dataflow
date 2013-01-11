package scala.dataflow.array

import scala.annotation.tailrec
import scala.dataflow.Future
import scala.reflect.ClassTag

/**
 * Hierarchical FlowArray
 *
 * This FlowArray holds an array of sub-FlowArrays and exposes it 
 * to the user as if it were flattened. Operations on HFAs produce 
 * truly flattened (i.e. FlatFlowArrays) FlowArrays again. HFAs are
 * used as the result of calls to flatMapN and flatten
 */
class HierFlowArray[A : ClassTag](
  /** the sub-FlowArrays of this HFA */
  private[array] val subData: Array[FlowArray[A]],
  /** the size of each individual sub-FlowArray */
  private[array] val subSize: Int
) extends ConcreteFlowArray[A] {

  import FlowArray._
  import SlicedJob._

  /** number of sub-FlowArrays, this HFA contains */
  private[array] val outerSize = subData.length

  override val size = subData.length * subSize

  /**
   * index of smallest indexed sub-FlowArray which has not yet seen
   * to be done
   */
  @volatile
  private var doneInd: Int = 0

  /**
   * Calculate sub-slices for sub-FlowArrays that are required to
   * compose a bigger slice.
   *
   * Example:
   *   The HFA has the following structure:
   *     0 1 2 | 3 4 5 | 5 6 8 | 9 10 11
   *   Suppose the requested slice is (4, 9):
   *               4 5 | 5 6 8 | 9
   *   SubSlices will return:
   *     (1, 1, 2), (2, 0, 2), (4, 0, 0)
   *   where the first element is the index of the sub-FlowArray, and
   *   the last two the slice in the sub-FlowArray
   */
  private[array] final def subSlices(from: Int, to: Int) = {
    val lbound = from/ subSize
    val ubound = to  / subSize
    for (i <- lbound to ubound)
      yield (i,
             if (i == lbound) from % subSize else 0,
             if (i == ubound) to   % subSize else subSize - 1
           )
  }

  private[array] override def sliceJobs(from: Int, to: Int): SliceDep = {

    /**
     * queries jobs of all sub-FlowArrays that are required for the
     * given slice
     */
    def rawSubFAJobs = {
      for { (i,l,u) <- subSlices(from, to)
                job <- subData(i).sliceJobs(l,u)
          } yield job
    }

    /**
     * reshape the rawSubFAJobs to meet SliceDep format
     */
    def subFAJobs = {
      val (js, redo) = rawSubFAJobs.unzip
      val fjs = js.flatten
      if (fjs.isEmpty)
        None
      else
        Some((fjs, redo.exists(x => x)))
    }

    super.sliceJobs(from, to).map(x => (x._1, true)) orElse subFAJobs
  }

  override private[array] final def dispatch(
      gen: JobGen,
      dstOffset: Int,
      srcOffset: Int,
      length: Int) = {
    val job = FADispatcherJob(this, gen, dstOffset, srcOffset, length)
    dispatch(job, srcOffset, length)
    job
  }

  override private[array] final def copyToArray(
      dst: Array[A],
      srcPos: Int,
      dstPos: Int,
      length: Int) {
    val li = srcPos / subSize
    val ld = srcPos % subSize
    for ((i, l, u) <- subSlices(srcPos, srcPos + length - 1)) {
      subData(i).copyToArray(dst, l, dstPos + (i - li)*subSize - ld + l, u - l + 1)
    }
  }

  /**
   * this HFA represented as a FFA.
   *
   * a FFA containing FAs, using the same data array and generation
   * job as this HFA
   */
  private lazy val asFFA: FlatFlowArray[FlowArray[A]] = {
    val fa = new FlatFlowArray(subData)
    fa.generatedBy(this)
    fa
  }

  /**
   * encapsulate and dispatch a fold-like job on this HFA
   *
   * @param from start of slice to operate on
   * @param to end of slice to operate on
   * @param z neutral element of accumulation
   * @param op associative accumulator
   * @param inner closure to map each internal FlowArray to a Future
   * which is to accumulate
   */
  @inline
  private def encFoldLike[B](from: Int, to: Int)
                            (z: B)
                            (op: (B,B) => B)
                            (inner: FlowArray[A] => FoldFuture[B]) = {
    val view = {
      if (from == 0 && to == size - 1) asFFA
      else asFFA.slice(from,to)
    }

    // Fold individual elements (emulate a map)
    val folds = view.mapToFFA(inner)
    
    // Consolidate
    val cjob = FAFoldConsolidateJob(folds, 0, folds.size)

    folds.dispatch(cjob, 0, folds.size)
    
    // Spawn last fold job
    val ajob = FAFoldJob(folds, 0, folds.size, z, op,
                         (x: FoldFuture[B]) => x.get)
    val fut = new FoldFuture(ajob)

    cjob.depending(ajob)

    fut
  }

  override def fold[A1 >: A](from: Int, to: Int)
                            (z: A1)
                            (op: (A1, A1) => A1): FoldFuture[A1] = 
    encFoldLike(from, to)(z)(op)(_.fold(z)(op))

  override def zipMapFold[B : ClassTag, C](from: Int, to: Int)
                                          (that: FlowArray[B])
                                          (f: (A,B) => C)
                                          (z: C)
                                          (op: (C,C) => C) =
    // TODO implement
    sys.error("not implemented yet")

  override def flatten[B](n: Int)
                         (implicit flat: CanFlatten[A,B],
                          mf: ClassTag[B]
                         ): FlowArray[B] = {
    // Somehow we have to pass the implicit by hand in the second call.
    // Compiler screws it up...
    asFFA.map(_.flatten(n)).flatten(subSize*n)(flattenFaInFa[B], mf)
  }

  override def blocking(isAbs: Boolean, msecs: Long): Array[A] = {
    block(isAbs, msecs)

    val ret = new Array[A](size)
    copyToArray(ret, 0, 0, size)
    ret
  }

  override final def unsafe(i: Int) =
    subData(i / subSize).unsafe(i % subSize) 

  @tailrec
  override final def jobDone() {
    // Sets flatMapJob to null
    setDone()

    if (done)
      freeBlocked()

    // It is OK to read doneInd here, since the first time this is called is when
    // the flatMap job (src) completes. Hence all FAs in subData are created.
    else if (!subData(doneInd).tryAddObserver(this))
      jobDone()
  }

  override def done = super.done && {
    val di = advDone()
    di >= subData.length
  }

  /**
   * try to advance index of smallest indexed sub-FlowArray which has
   * not yet seen to be done.
   * @return index that has been seen to be not yet done
   */
  private def advDone() = {
    var i = /*READ*/doneInd
    while (i < subData.length && subData(i).done) { i += 1 }
    doneInd/*WRITE*/ = i
    i
  }

}
