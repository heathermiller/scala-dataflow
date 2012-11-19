package scala.dataflow.array

import scala.annotation.tailrec

class HierFlowArray[A : ClassManifest](
  private[array] val subData: Array[FlowArray[A]],
  private[array] val subSize: Int
) extends FlowArray[A] with FAJob.Observer {

  // Fields
  val size = subData.length * subSize

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
    subData.foreach(_.block())

    val ret = new Array[A](size)
    copyToArray(ret, 0)
    ret
  }

  override def done = super.done && subData.forall(_.done)

}
