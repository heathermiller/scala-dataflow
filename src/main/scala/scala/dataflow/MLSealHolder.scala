package scala.dataflow

import scala.annotation.tailrec

class MLSealHolder {
  import MLSealHolder._

  @volatile var s: State = Unsealed
}

object MLSealHolder {

  sealed abstract class State
  
  final case class Proposition(val size: Int) extends State
  final case class MLSeal(size: Int, remain: Int) extends State
  final case object Unsealed extends State

}
