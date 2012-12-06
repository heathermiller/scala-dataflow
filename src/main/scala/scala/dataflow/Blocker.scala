package scala.dataflow

import scala.annotation.tailrec
import java.util.Date

abstract class Blocker {

  import Blocker._

  // Abstract
  def done: Boolean

  // Internals
  @volatile private var waiting: WaitList = Empty

  // Unsafe stuff
  private val unsafe = getUnsafe()
  private val OFFSET = unsafe.objectFieldOffset(classOf[Blocker].getDeclaredField("waiting"))
  @inline private def CAS(ov: WaitList, nv: WaitList) =
    unsafe.compareAndSwapObject(this, OFFSET, ov, nv)

  // Exposed to mixer
  @tailrec
  final protected def freeBlocked() {
    @tailrec
    def free0(w: WaitList): Unit = w match {
      case Blocking(thr, next) =>
        unsafe.unpark(thr)
        free0(next)
      case Empty | Complete => 
    }

    val ov = /*READ*/waiting

    if (!CAS(ov, Complete))
      freeBlocked()
    else
      free0(ov)
  }

  private final def mnow = (new Date()).getTime

  final protected def block(isAbs: Boolean = false, msecs: Long = 0) {

    val isTimed  = isAbs || msecs > 0
    val until = if (isAbs) msecs else mnow + msecs

    def timeOver = mnow >= until

    def park() { 
      if (isTimed)  unsafe.park(true, until)
      else          unsafe.park(false, 0)
    }

    @tailrec
    def block0() {
      val curo = /*READ*/waiting

      if (!done && curo != Complete) {
        val nv = Blocking(Thread.currentThread, curo)

        if (CAS(curo, nv)) {
          park()

          if (isTimed && timeOver)
            throw new InterruptedException()
        }

        block0()
      }
    }

    block0()
  }

}

object Blocker {
  sealed abstract class WaitList
  case object Empty    extends WaitList
  case object Complete extends WaitList
  case class  Blocking(thr: Thread, next: WaitList) extends WaitList
}
