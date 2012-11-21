package scala.dataflow.array

import scala.dataflow.Future
import scala.annotation.tailrec

class FoldFuture[A](z: A, f: (A, A) => A) extends Future[A] with FAJob.Observer {

  @volatile private var result: A = z

  private val unsafe = getUnsafe()
  private val OFFSET =
    unsafe.objectFieldOffset(classOf[FoldFuture[_]].getDeclaredField("result"))
  @inline private def CAS(ov: A, nv: A) =
    unsafe.compareAndSwapObject(this, OFFSET, ov, nv)

  @tailrec
  private[array] final def acc(x: A) {
    val ov = /*READ*/result
    val nv = f(ov, x)
    if (!CAS(ov, nv)) acc(x)
  }

  override def jobDone() { tryComplete(/*READ*/result) }

}

