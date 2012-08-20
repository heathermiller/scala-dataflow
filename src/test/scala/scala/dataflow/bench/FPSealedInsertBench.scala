package scala.dataflow.bench

import scala.dataflow._

trait FPSealedInsertBench extends testing.Benchmark with Utils.Props with FPBuilder {
  import Utils._
  
  override def run() {
    val pool = newFP[Data]
    val builder = pool.builder
    val work = size / par
    val data = new Data(0)

    // Seal the FP first
    builder.seal(work)

    val writers = for (ti <- 1 to par) yield task {
      var i = 0
      while (i < work) {
        builder += data
        i += 1
      }
    }

    writers.foreach(_.join())

  }
  
}

