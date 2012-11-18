package scala.dataflow

import scala.collection.parallel.mutable.ParArray


object PABench extends testing.Benchmark {

  val no = 1000
  val ni = 10000

  def run {
    val pa1 = ParArray.tabulate(no)(x => x*x)
    val pa2 = pa1.map(_ * 2)
    val pa3 = pa2.map(_ / 2.34)
    val pa4 = pa3 flatMap { x =>
      ParArray.tabulate(ni)(y => x * y)
    }
    val pa5 = pa4.map(_ / 1.2)

    println(pa5(30))
  }

}
