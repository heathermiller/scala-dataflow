package scala.dataflow.array

import scala.annotation.tailrec

class HierFlowArray[A : ClassManifest](
  private[array] val subData: Array[FlowArray[A]],
  private[array] val subSize: Int
) extends ConcreteFlowArray[A] with Blocker {

  import FlowArray._

  // Fields
  private[array] val outerSize = subData.length
  val size = subData.length * subSize

  @volatile var doneInd: Int = 0

  // Slice-wise dependencies
  private[array] override def sliceJobs(from: Int, to: Int): SliceDep = {
    def subSlices = {
      val lbound = from/ subSize
      val ubound = to  / subSize
      for (i <- lbound to ubound)
        yield (i,
               if (i == lbound) from % subSize else 0,
               if (i == ubound) to   % subSize else subSize - 1
             )
    }

    def rawSubFAJobs = {
      val subs = subSlices
      for { (i,l,u) <- subSlices
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

  private[array] final def dispatch(gen: JobGen, offset: Int): FAJob = {
    val job = FADispatcherJob(this, gen, offset)
    dispatch(job)
    job
  }

  final private[array] def copyToArray(trg: Array[A], offset: Int) {
    for ((sd, i) <- subData.zipWithIndex) {
      sd.copyToArray(trg, offset + i*subSize)
    }
  }

  override def blocking(isAbs: Boolean, msecs: Long): Array[A] = {
    block(isAbs, msecs)

    val ret = new Array[A](size)
    copyToArray(ret, 0)
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
