package scala.dataflow.bench

import scala.dataflow._

// Par level: Number of maps you do, single writer
trait FPMapBench extends testing.Benchmark with Utils.Props with FPBuilder {
  import Utils._
  
  override def run() {
    val pool = newFP[Data]
    val builder = pool.builder
    val work = size / par
    val data = new Data(0)

    val res = pool.mappedFold(0)(_ + _)(_.i)
    
    for (ti <- 1 to par) yield task {
      var i = 0
      while (i < work) {
        builder << data
        i += 1
      }
    }

    builder.seal(size)

    res.blocking

  }
  
}
