package scala.dataflow

import scala.collection.parallel.mutable.ParArray


object PABench extends testing.Benchmark {

  val pa1 = ParArray.tabulate(1000000)(x => x*x)

  def run {
    val pa2 = pa1.map(_ * 2)
    val pa3 = pa2.map(_ / 2.34)
    val pa4 = pa3.map(_ / 1.2)

    println(pa4(30))
  }

}
