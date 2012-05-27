package scala.dataflow

import scala.annotation.tailrec

class MLSealHolder {
  import MLSealHolder._

  @volatile var s: State = Unsealed
}

object MLSealHolder {

  def STEAL_CNT = FlowPool.MAX_BLOCK_ELEMS

  final class StealState(
    val rem:    Int,
    val stolen: Map[Int,Int] 
  ) {
    def stolenFor(bli: Int) = {
      val nv = math.max(0, rem - STEAL_CNT)
      new StealState(nv, stolen + (bli -> (rem - nv)))
    }
    def commitedFor(bli: Int) =
      new StealState(rem, stolen - bli)
  }

  sealed abstract class State
  
  final case class Proposition(val size: Int) extends State
  final class MLSeal(
    val size: Int,
    remain: Int
  ) extends State {

    private val unsafe = getUnsafe()
    private val SS_OFFSET =
      unsafe.objectFieldOffset(classOf[MLSeal].getDeclaredField("stealState"))
    @inline private def CAS_SS(ov: StealState, nv: StealState) =
      unsafe.compareAndSwapObject(this, SS_OFFSET, ov, nv)

    @volatile var stealState = new StealState(remain, Map.empty)

    def stageSteal(bli: Int): StealState = {
      val st = /*READ*/stealState
      
      if (!st.stolen.contains(bli)) {
        val nst = st.stolenFor(bli)
        if (CAS_SS(st, nst)) nst
        else stageSteal(bli)
      } else st
    }

    @tailrec
    def commitSteal(bli: Int) {
      val st = /*READ*/stealState
      val nst = st.commitedFor(bli)
      if (!CAS_SS(st,nst))
        commitSteal(bli)
    }

  }
  object Unsealed extends State

  object MLSeal {
    def unapply(s: MLSeal): Option[Int] = Some(s.size)
  }

}
