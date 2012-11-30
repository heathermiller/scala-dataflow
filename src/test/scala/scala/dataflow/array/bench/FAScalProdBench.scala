package scala.dataflow.array.bench

import scala.dataflow.Utils
import scala.dataflow.array._

object FAScalProdBench extends testing.Benchmark with Utils.Props {

  def run {
    val x = FlowArray.tabulate(size)(x => x*x)
    val y = FlowArray.tabulate(size)(x => x*x)

    val p = (x zipMap y) (_ * _)
    
    val res = p.fold(0)(_ + _)

    println(res.blocking)
  }

}
