package scala.dataflow.array

import scala.dataflow.Future
import scala.annotation.tailrec
import scala.reflect.ClassTag

/**
 * A concrete FlowArray that holds its data directly in an internal array
 */
class FlatFlowArray[A : ClassTag](
  /** data of this FFA */
  private[array] val data: Array[A]
) extends ConcreteFlowArray[A] {

  import FlowArray._

  override val size = data.length

  override private[array] final def dispatch(
      gen: JobGen,
      dstOffset: Int,
      srcOffset: Int,
      length: Int) = {
    val job = gen(this, dstOffset, srcOffset, length)
    dispatch(job, srcOffset, length)
    job
  }

  override private[array] final def copyToArray(
      dst: Array[A],
      srcPos: Int,
      dstPos: Int,
      length: Int) {
    Array.copy(data, srcPos, dst, dstPos, length)
  }

  override def fold[A1 >: A](from: Int, to: Int)
                            (z: A1)
                            (op: (A1, A1) => A1): FoldFuture[A1] = {
    val fsize = to - from + 1
    val job = FAFoldJob(this, from, fsize, z, op)
    val fut = new FoldFuture(job)
    dispatch(job, from, fsize)
    fut
  }

  override def flatten[B](n: Int)(implicit flat: CanFlatten[A,B],
                                  mf: ClassTag[B]): FlowArray[B] =
    flat.flatten(this, n)

  override def zipMapFold[B : ClassTag, C](from: Int, to: Int)
                                          (that: FlowArray[B])
                                          (f: (A,B) => C)
                                          (z: C)
                                          (op: (C,C) => C) = {
    val fsize = to - from + 1
    val job = FAZipMapFoldJob(this, that, f, z, op, from, 0, fsize)
    val fut = new FoldFuture(job)
    dispatch(job, from, fsize)
    fut
  }

  override def jobDone() {
    setDone()
    freeBlocked()
  }

  override def blocking(isAbs: Boolean, msecs: Long): Array[A] = {
    block(false, msecs)
    data
  }

  override final def unsafe(i: Int) = data(i)

}
