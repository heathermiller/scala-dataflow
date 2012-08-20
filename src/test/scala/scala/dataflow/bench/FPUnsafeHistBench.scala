package scala.dataflow.bench

import scala.dataflow._
import java.util.concurrent.ThreadLocalRandom

trait FPUnsafeHistBench extends testing.Benchmark with Utils.Props with FPBuilder {
  import Utils._

  val maxval = 100
  
  override def run() {
    val pool = newFP[Data]
    val builder = pool.builder
    val work = size / par
    val bins = 5 to 20
    def data = new Data(ThreadLocalRandom.current.nextInt(maxval))

    val res = bins.map(s => binning(s,pool))

    for (ti <- 1 to par) yield task {
      var i = 0
      while (i < work) {
        builder += data
        i += 1
      }
    }

    builder.seal(size)

    res.foreach(_.blocking)

  }

  private def binning(count: Int, pool: FlowPool[Data]) = {
    // this is fast but unsafe
    val agg = Array.fill[Int](count)(0)
    pool.foreach { x =>
      val ind = x.i * count / maxval
      agg(ind) = agg(ind) + 1            
    } map { x => agg }
  }
  
}
