package scala.dataflow.bench

import scala.dataflow._
import scala.util.Random

object FPUnsafeHistBench extends testing.Benchmark with Utils.Props {
  import Utils._

  val maxval = 100
  
  override def run() {
    val pool = new FlowPool[Data]()
    val builder = new Builder[Data](pool.initBlock)
    val work = size
    val bins = 5 to 20
    def data = new Data(Random.nextInt(maxval))
    var i = 0

    val res = bins.map(s => binning(s,pool))
    
    while (i < work) {
      builder << data
      i += 1
    }

    builder.seal(work)

    res.foreach(_.blocking)

  }

  private def binning(count: Int, pool: FlowPool[Data]) = {
    // this is fast but unsafe
    val agg = Array.fill[Int](count)(0)
    pool.doForAll { x =>
      val ind = x.i * count / maxval
      agg(ind) = agg(ind) + 1            
    } map { x => agg }
  }
  
}
