package scala.dataflow.bench

import scala.dataflow._

trait FPSealedInsertBench extends testing.Benchmark with Utils.Props with FPBuilder {
  import Utils._
  
  override def run() {
    val pool = newFP[Data]
    val builder = pool.builder
    val work = size
    val data = new Data(0)
    var i = 0

    // Seal the FP first
    builder.seal(work)
    
    while (i < work) {
      builder << data
      i += 1
    }
  }
  
}

