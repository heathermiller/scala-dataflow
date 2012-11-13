package scala.dataflow.array

import scala.dataflow.Future
import scala.annotation.tailrec

abstract class FlowArray[A : ClassManifest] {

  import FlowArray._

  // Fields
  def size: Int
  def length = size

  // Internals
  @volatile private var waiting: WaitList = Empty

  // Unsafe stuff
  private val unsafe = getUnsafe()
  private val OFFSET = unsafe.objectFieldOffset(classOf[FlowArray[_]].getDeclaredField("waiting"))
  @inline private def CAS(ov: WaitList, nv: WaitList) =
    unsafe.compareAndSwapObject(this, OFFSET, ov, nv)

  // Public members
  def map[B : ClassManifest](f: A => B): FlowArray[B]
  def flatMapN[B : ClassManifest](n: Int)(f: A => FlowArray[B]): FlowArray[B]
  def mutConverge(cond: A => Boolean)(it: A => Unit): FlowArray[A]
  def converge(cond: A => Boolean)(it: A => A): FlowArray[A]
  def fold[A1 >: A](z: A1)(op: (A1, A1) => A1): Future[A1]

  def done: Boolean
  def blocking: Array[A]

  // Implementations

  @tailrec
  final protected def freeBlocked(): Unit = /*READ*/waiting match {
    case Empty => 
      if (!CAS(Empty, Complete))
        freeBlocked()
    case ov@Blocking(thr, next) =>
      if (CAS(ov, next)) {
        unsafe.unpark(thr)
        freeBlocked()
      } else freeBlocked()
    case Complete =>
      /* this may sporadically happen */
  }

  @tailrec
  final def block() {
    val curo = /*READ*/waiting

    if (!done && curo != Complete) {
      val nv = Blocking(Thread.currentThread, curo)
      if (CAS(curo, nv))
        unsafe.park(false, 0)

      block
    }
  }

}

object FlowArray {

  sealed abstract class WaitList
  case object Empty    extends WaitList
  case object Complete extends WaitList
  case class  Blocking(thr: Thread, next: WaitList) extends WaitList

  def tabulate[A : ClassManifest](n: Int)(f: Int => A) = {
    val ret = new FlatFlowArray(new Array[A](n))
    val job = FAGenerateJob(ret, f, ret)
    ret.srcJob = job
    FAJob.schedule(job)
    ret
  }

}
