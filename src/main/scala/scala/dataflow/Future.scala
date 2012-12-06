package scala.dataflow

import scala.annotation.tailrec
import scala.collection.mutable.MutableList

class Future[T] extends Blocker {

  import Future._

  private val unsafe = getUnsafe()
  private val OFFSET =
    unsafe.objectFieldOffset(classOf[Future[_]].getDeclaredField("state"))
  @inline private def CAS(ov: State[T,T], nv: State[T,T]) =
    unsafe.compareAndSwapObject(this, OFFSET, ov, nv)

  @volatile private var state: State[T,T] = CBNil

  /** up to you to check completion! */ 
  def get = getOption.get
  def getOption = /*READ*/state match {
    case Done(v) => Some(v)
    case _ => None
  }

  def blocking(isAbs: Boolean, msecs: Long): T = {
    block(isAbs, msecs)
    get
  }

  def blocking: T = blocking(false, 0)

  def map[U](f: T => U): Future[U] = {
    val fut = new Future[U]
    registerCB(x => fut.complete(f(x)))
    fut
  }

  def flatMap[U](f: T => Future[U]): Future[U] = {
    val fut = new Future[U]
    registerCB { x => f(x).foreach(fut.complete _) }
    fut
  }

  def andThen[U](body: => U) = {
    val fut = new Future[T]
    registerCB { x =>
      body
      fut.complete(x)
    }
    fut
  }

  def foreach[U](f: T => U) {
    registerCB(x => f(x))
  }

  def done = /*READ*/state match {
    case Done(_) => true
    case _ => false
  }

  @tailrec
  private def registerCB(f: T => Unit): Unit = /*READ*/state match {
    case cb: CBList[T] =>
      if (!CAS(cb, CBElem(f, cb)))
        registerCB(f)
    case Done(v) => f(v)
  }
  
  final private[dataflow] def complete(v: T) {
    if (!tryComplete(v))
      sys.error("Future completed twice")
  }

  @tailrec
  final private[dataflow] def tryComplete(v: T): Boolean = /*READ*/state match {
    case cb: CBList[T] =>
      if (CAS(cb, Done(v))) {
        freeBlocked()
        execCBS(v, cb)
        true
      } else tryComplete(v)
    case Done(_) =>
      false
  }

  @tailrec
  private def execCBS(v: T, cbs: CBList[T]): Unit = cbs match {
    case CBElem(f, n) => f(v); execCBS(v, n)
    case CBNil => 
  }

}

object Future {
  abstract class State[+S,-T]
  abstract class CBList[-T] extends State[Nothing,T]
  case class CBElem[-T](f: T => Unit, n: CBList[T]) extends CBList[T]
  case object CBNil extends CBList[Any]
  case class Done[+S](v: S) extends State[S,Any]
}
