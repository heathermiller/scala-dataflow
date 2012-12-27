package scala.dataflow.array.bench

import scala.dataflow.Utils
import scala.dataflow.array._

object FAScalProdBenchZMF extends testing.Benchmark with Utils.Props {

  FlowArray.setPar(par)

  def run {
    val x = FlowArray.tabulate(size)(x => x*x)
    val y = FlowArray.tabulate(size)(x => x*x)

    val res = (x zipMapFold y)(_ * _)(0)(_ + _)

    res.blocking
  }

}
