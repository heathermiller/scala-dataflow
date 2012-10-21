package scala.dataflow

import array._

object FAFibBench extends testing.Benchmark {

  val raw = Array.tabulate(40)(x => x)
  val fa1 = new FlowArray(raw)

  // Stupid fib implementation to have value dependent task time
  def fib(i: Int): Int =
    if (i == 0) 1
    else if (i == 1) 1
    else fib(i-1) + fib(i-2)
  
  def run {

    val fa2 = fa1.map(x => (x,fib(x)))
    val fa3 = fa2.map(x => fib(40 - x._1))

    println(fa3.blocking(30))
  }

}

