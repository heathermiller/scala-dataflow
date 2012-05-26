package scala.dataflow

import scala.annotation.tailrec

class MLSealHolder {
  import MLSealHolder._

  @volatile var s: State = Unsealed
}

object MLSealHolder {

  def STEAL_CNT = 256

  sealed abstract class State
  
  final case class Proposition(val size: Int) extends State
  final class MLSeal(
    val size: Int,
    remain: Int
  ) extends State {

    private val unsafe = getUnsafe()
    private val REM_OFFSET =
      unsafe.objectFieldOffset(classOf[MLSeal].getDeclaredField("remaining"))
    private val STL_OFFSET =
      unsafe.objectFieldOffset(classOf[MLSeal].getDeclaredField("remaining"))
    @inline private def CAS(ov: Int, nv: Int) =
      unsafe.compareAndSwapInt(this, REM_OFFSET, ov, nv)

    @volatile var remaining: Int = remain
    @volatile var stealing: Int = 0

    @tailrec
    def stageSteal(): Int = {
      // TODO --> we have issues here! (how to steal without blocking and busy-waiting)
      val cstl = /*READ*/stealing
      val crem = /*READ*/remaining
      if (crem > 0) {
        val nv = math.max(0, crem - STEAL_CNT)
        if (CAS(crem, nv)) crem - nv
        else stageSteal()
      } else 0
    }


    def commitSteal(c: Int) = {

    }


    def revertSteal(c: Int) = {

    }

  }
  object Unsealed extends State

  object MLSeal {
    def unapply(s: MLSeal): Option[Int] = Some(s.size)
  }

}
