package scala.dataflow.array.bench

import scala.dataflow.Utils
import scala.collection.parallel.mutable.ParArray

object PAScalProdBench extends testing.Benchmark with Utils.Props {

  def run {
    val x = ParArray.tabulate(size)(x => x*x)
    val y = ParArray.tabulate(size)(x => x*x)

    val p = (x zip y).map(x => x._1 * x._2)
    
    val res = p.fold(0)(_ + _)

    println(res)
  }

}
