package scala.dataflow.array

import scala.annotation.tailrec
import scala.dataflow.Future

class HierFlowArray[A : ClassManifest](
  private[array] val subData: Array[FlowArray[A]],
  private[array] val subSize: Int
) extends ConcreteFlowArray[A] {

  import FlowArray._
  import SlicedJob._

  // Fields
  private[array] val outerSize = subData.length
  val size = subData.length * subSize

  @volatile var doneInd: Int = 0

  final private[array] def subSlices(from: Int, to: Int) = {
    val lbound = from/ subSize
    val ubound = to  / subSize
    for (i <- lbound to ubound)
      yield (i,
             if (i == lbound) from % subSize else 0,
             if (i == ubound) to   % subSize else subSize - 1
           )
  }

  // Slice-wise dependencies
  private[array] override def sliceJobs(from: Int, to: Int): SliceDep = {

    def rawSubFAJobs = {
      for { (i,l,u) <- subSlices(from, to)
             job <- subData(i).sliceJobs(l,u)
          } yield job
    }

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

  final private[array] def dispatch(gen: JobGen, dstOffset: Int, srcOffset: Int, length: Int) = {
    val job = FADispatcherJob(this, gen, dstOffset, srcOffset, length)
    dispatch(job, srcOffset, length)
    job
  }

  final private[array] def copyToArray(dst: Array[A], srcPos: Int, dstPos: Int, length: Int) {
    val li = srcPos / subSize
    val ld = srcPos % subSize
    for ((i, l, u) <- subSlices(srcPos, srcPos + length - 1)) {
      subData(i).copyToArray(dst, l, dstPos + (i - li)*subSize - ld + l, u - l + 1)
    }
  }

  private lazy val asFFA = {
    val fa = new FlatFlowArray(subData)
    fa.generatedBy(this)
    fa
  }

  def fold[A1 >: A](from: Int, to: Int)(z: A1)(op: (A1, A1) => A1): FoldFuture[A1] = {
    val view = {
      if (from == 0 && to == size - 1) asFFA
      else asFFA.slice(from,to)
    }

    // Fold individual elements (emulate a map)
    val folds = view.mapToFFA(_.fold(z)(op))
    
    // Consolidate
    val cjob = FAFoldConsolidateJob(folds, 0, folds.size)

    folds.dispatch(cjob, 0, folds.size)
    
    // Spawn last fold job
    val ajob = FAFoldJob(folds, 0, folds.size, z, op, (x: FoldFuture[A1]) => x.get)
    val fut = new FoldFuture(ajob)

    cjob.depending(ajob)

    fut
  }

  def flatten[B](n: Int)(implicit flat: CanFlatten[A,B], mf: ClassManifest[B]): FlowArray[B] = {
    // Somehow we have to pass the implicit by hand in the second call.
    // Compiler screws it up...
    asFFA.map(_.flatten(n)).flatten(subSize*n)(flattenFaInFa[B], mf)
  }

  def transpose(from: Int, to: Int)(step: Int) = {
    val len = to - from + 1
    val ret = new FlatFlowArray(new Array[A](len))

    val tjobg = FATransposeJob(ret, step, from, len)

    val tjob = dispatch(tjobg, 0, from, len)

    val ajob = FAAlignJob(tjob, 0, len)
    FAJob.schedule(ajob)

    ret.generatedBy(ajob)
    ret
  }

  override def blocking(isAbs: Boolean, msecs: Long): Array[A] = {
    block(isAbs, msecs)

    val ret = new Array[A](size)
    copyToArray(ret, 0, 0, size)
    ret
  }

  final def unsafe(i: Int) = subData(i / subSize).unsafe(i % subSize) 

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

  private def advDone() = {
    var i = /*READ*/doneInd
    while (i < subData.length && subData(i).done) { i += 1 }
    doneInd/*WRITE*/ = i
    i
  }

}
