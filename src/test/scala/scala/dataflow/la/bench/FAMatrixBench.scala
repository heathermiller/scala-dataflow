package scala.dataflow.la.bench

import scala.dataflow.Utils
import scala.dataflow.la._

object FAMatrixBench extends testing.Benchmark with Utils.Props {

  def run {
    val m = Matrix.ones(size,size)
    val v = Vector.ones(size)
    val res = m * v

    println((res * res).blocking)
  }

}
