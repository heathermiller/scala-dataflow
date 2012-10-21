package scala.dataflow

import array._

object FABench extends testing.Benchmark {

  val raw = Array.tabulate(1000000)(x => x*x)
  val fa1 = new FlowArray(raw)

  def run {

    val fa2 = fa1.map(_ * 2)
    val fa3 = fa2.map(_ / 2.34)
    val fa4 = fa3.map(_ / 1.2)

    println(fa4.blocking(30))
  }

}

