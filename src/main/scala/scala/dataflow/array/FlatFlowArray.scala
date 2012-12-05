package scala.dataflow.array

import scala.dataflow.Future
import scala.annotation.tailrec

class FlatFlowArray[A : ClassManifest](
  private[array] val data: Array[A]
) extends ConcreteFlowArray[A] {

  import FlowArray._

  // Fields
  val size = data.length

  final private[array] def dispatch(gen: JobGen, dstOffset: Int, srcOffset: Int, length: Int) = {
    val job = gen(this, dstOffset, srcOffset, length)
    dispatch(job, srcOffset, length)
    job
  }

  final private[array] def copyToArray(dst: Array[A], srcPos: Int, dstPos: Int, length: Int) {
    Array.copy(data, srcPos, dst, dstPos, length)
  }

  def fold[A1 >: A](from: Int, to: Int)(z: A1)(op: (A1, A1) => A1): FoldFuture[A1] = {
    val fsize = to - from + 1
    val job = FAFoldJob(this, from, fsize, z, op)
    val fut = new FoldFuture(job)
    dispatch(job, from, fsize)
    fut
  }

  def flatten[B](n: Int)(implicit flat: CanFlatten[A,B], mf: ClassManifest[B]): FlowArray[B] =
    flat.flatten(this, n)

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
