package scala.dataflow

import array._

object FATest extends App {

  val raw = Array.tabulate(100000)(x => x*x)

  val fa1 = new FlowArray(raw)
  val fa2 = fa1.map(_ * 2)

  println(fa2.blocking(30))

}
