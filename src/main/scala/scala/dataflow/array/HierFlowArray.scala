package scala.dataflow.array

import scala.annotation.tailrec

class HierFlowArray[A : ClassManifest](
  private[array] val subData: Array[FlowArray[A]],
  private[array] val subSize: Int
) extends FlowArray[A] {

  // Fields
  val outerSize = subData.length
  val size = subData.length * subSize

  @volatile var doneInd: Int = 0

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

  def blocking: Array[A] = {
    block()

    val ret = new Array[A](size)
    copyToArray(ret, 0)
    ret
  }

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
