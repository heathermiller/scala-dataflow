package scala.dataflow.bench

import scala.dataflow._

trait FPInsertBench extends testing.Benchmark with Utils.Props with FPBuilder {
  import Utils._
  
  override def run() {
    val pool = newFP[Data]
    val builder = pool.builder
    val work = size
    val data = new Data(0)
    var i = 0
    
    while (i < work) {
      builder << data
      i += 1
    }

    // Seal FP to make it comparable to FPSealedInsertBench
    builder.seal(work)

  }
  
}
