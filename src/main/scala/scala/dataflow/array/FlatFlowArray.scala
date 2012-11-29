package scala.dataflow.array

import scala.annotation.tailrec

class FlatFlowArray[A : ClassManifest](
  private[array] val data: Array[A]
) extends ConcreteFlowArray[A] {

  import FlowArray._

  // Fields
  val size = data.length

  final private[array] def dispatch(gen: JobGen, offset: Int) = {
    val job = gen(this, offset, 0, size)
    dispatch(job)
    job
  }

  final private[array] def copyToArray(dst: Array[A], srcPos: Int, dstPos: Int, length: Int) {
    Array.copy(data, srcPos, dst, dstPos, length)
  }

  override def jobDone() {
    setDone()
    freeBlocked()
  }

  override def blocking(isAbs: Boolean, msecs: Long): Array[A] = {
    block(false, msecs)
    data
  }

  final def unsafe(i: Int) = data(i)

}
