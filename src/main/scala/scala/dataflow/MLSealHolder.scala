package scala.dataflow

class MLSealHolder {
  import MLSealHolder._

  @volatile var s: State = Unsealed
}

object MLSealHolder {

  sealed abstract class State
  
  final case class Proposition(val size: Int) extends State
  final class MLSeal(
    val size: Int,
    remain: Int
  ) extends State {
    @volatile var remaining = remain
  }
  object Unsealed extends State

  object MLSeal {
    def unapply(s: MLSeal): Option[Int] = Some(s.size)
  }

}
