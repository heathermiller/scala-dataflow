package scala.dataflow.array.bench

import scala.dataflow.Utils
import scala.dataflow.array._
import org.scalameter.api._

object FAScalProdBench extends PerformanceTest {

  lazy val executor = SeparateJvmsExecutor(
    org.scalameter.Executor.Warmer.Default(),
    Aggregator.min,
    new Measurer.Default
  )

  lazy val reporter  = new LoggingReporter
  lazy val persistor = Persistor.None

  val sizes = Gen.range("size")(1000, 10000, 2000)

  performance of "FlowArray" in {
    measure method "Scalar Product" in {
      using(sizes) in { sz => scp(sz) }
    }
  }

  private def scp(size: Int) {
    val x = FlowArray.tabulate(size)(x => x*x)
    val y = FlowArray.tabulate(size)(x => x*x)

    val p = (x zipMap y) (_ * _)
    
    val res = p.fold(0)(_ + _)

    res.blocking
  }

}
