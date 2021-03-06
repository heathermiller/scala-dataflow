package scala.dataflow.array.bench

import scala.dataflow.Utils
import scala.dataflow.array._

object FAScalProdBench extends testing.Benchmark with Utils.Props {

  FlowArray.setPar(par)

  def run {
    val x = FlowArray.tabulate(size)(x => x*x)
    val y = FlowArray.tabulate(size)(x => x*x)

    val p = (x zip y).map(x => x._1 * x._2)
    
    val res = p.fold(0)(_ + _)

    res.blocking
  }

}
