package scala.dataflow

import scala.collection.parallel.mutable.ParArray
import array._

object FABench extends testing.Benchmark {

  val no = 1000
  val ni = 10000

  def run {
    val fa1 = FlowArray.tabulate(no)(x => x*x)
    val fa2 = fa1.map(_ * 2)
    val fa3 = fa2.map(_ / 2.34)
    val fa4 = (fa3 flatMapN ni) { x =>
      FlowArray.tabulate(ni)(y => x *y)
    }
    val fa5 = fa4.map(_ / 1.2)
    val fut = fa5.fold(0.0)(_ + _)

    println(fut.blocking)
  }

  override def tearDown {
    FAJob.printStats()
    FAJob.resetStats()
  }

}

