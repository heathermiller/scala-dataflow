package scala.dataflow.array.bench

import scala.dataflow.array._
import scala.dataflow.Utils

object FAFibBench extends testing.Benchmark with Utils.Props {

  FlowArray.setPar(par)

  // Stupid fib implementation to have value dependent task time
  def fib(i: Int): Int =
    if (i == 0) 1
    else if (i == 1) 1
    else fib(i-1) + fib(i-2)
  
  def run {
    val fa1 = FlowArray.tabulate(size)(x => x)
    val fa2 = fa1.map(x => (x,fib(x)))
    val fa3 = fa2.map(x => fib(size - x._1))

    fa3.blocking
  }

}

