package scala.dataflow.array

import scala.annotation.tailrec

class HierFlowArray[A : ClassManifest](
  private[array] val subData: Array[FlowArray[A]],
  private[array] val subSize: Int
) extends ConcreteFlowArray[A] {

  import FlowArray._

  // Fields
  private[array] val outerSize = subData.length
  val size = subData.length * subSize

  @volatile var doneInd: Int = 0

  private def subSlices(from: Int, to: Int) = {
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
      subData(i).copyToArray(dst, l, dstPos + (i - li)*subSize - ld, u - l + 1)
    }
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
    setDone()
    if (done)
      freeBlocked()
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
