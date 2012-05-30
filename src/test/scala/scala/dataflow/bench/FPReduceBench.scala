package scala.dataflow.bench

import scala.dataflow._

trait FPReduceBench extends testing.Benchmark with Utils.Props with FPBuilder {
  import Utils._
  
  override def run() {
    val pool = newFP[Data]
    val builder = pool.builder
    val work = size / par
    val data = new Data(0)

    val res = pool.aggregate(0)(_ + _)(_ + _.i)
    
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
