package scala.dataflow

import array._

object FATest extends App {

  val raw = Array.tabulate(20)(x => x*x toDouble)

  val fa1 = new FlowArray(raw)
  val fa2 = fa1.converge(_ <= 1)(_ / 2)
  val f   = fa2.fold(0.0)(_ + _)

  println(f.blocking)

}
