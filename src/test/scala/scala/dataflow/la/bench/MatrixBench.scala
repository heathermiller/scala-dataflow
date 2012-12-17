package scala.dataflow.la.bench

import scala.dataflow.Utils
import scala.dataflow.la._

trait MatrixBench extends testing.Benchmark with Utils.Props {
  this: LAImpl =>

  def run {
    val n = this.ones(size,size)
    val m = this.ones(size,size)
    val v = this.ones(size)
    val res = (n * m) * v
    println((res * res).blocking)
  }

}
