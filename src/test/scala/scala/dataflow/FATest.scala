package scala.dataflow

import array._

object FATest extends App {

  val n = 300

  val raw = Array.tabulate(n)(x => x*x)

  val fa1 = new FlowArray(raw)
  val fa2 = fa1.flatMapN(n)(mkFA _)

  println(fa2.blocking(n*n-1))

  def mkFA(v: Int) = {
    val raw = Array.tabulate(n)(x => x*v)
    new FlowArray(raw)
  }

}
