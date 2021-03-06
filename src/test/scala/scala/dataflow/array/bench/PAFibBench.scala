package scala.dataflow.array.bench

import scala.dataflow.Utils
import scala.collection.parallel.mutable.ParArray

object PAFibBench extends testing.Benchmark with Utils.Props {

  // Stupid fib implementation to have value dependent task time
  def fib(i: Int): Int =
    if (i == 0) 1
    else if (i == 1) 1
    else fib(i-1) + fib(i-2)

  def run {
    val pa1 = ParArray.tabulate(size)(x => x)
    val pa2 = pa1.map(x => (x,fib(x)))
    val pa3 = pa2.map(x => fib(size - x._1))
  }

}
