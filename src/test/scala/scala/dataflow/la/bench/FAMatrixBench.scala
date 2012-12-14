package scala.dataflow.la.bench

import scala.dataflow.Utils
import scala.dataflow.la._

object FAMatrixBench extends testing.Benchmark with Utils.Props with ArrayLA with FlowArrayImpl { 

  def run {
    val m = this.ones(size,size)
    val v = this.ones(size)
    val res = m * v

    println((res * res).blocking)
  }

}
