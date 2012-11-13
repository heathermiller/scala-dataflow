package scala.dataflow

import array._

object FATest extends App {

  val n = 4000

  val raw = Array.tabulate(n)(x => x*x toLong)

  val fa1 = new FlowArray(raw)
  val fa2 = fa1.flatMapN(n)(mkFA _)
  //val fa3 = fa2.map(_ / 2)
  //val fut = fa3.fold(0L)(_ + _)

  println(fa2.blocking(0))

  def mkFA(v: Long) = {
    val raw = Array.tabulate(n)(x => x*v)
    new FlowArray(raw)
  }

}
