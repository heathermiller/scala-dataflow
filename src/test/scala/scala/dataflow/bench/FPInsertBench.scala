package scala.dataflow.bench

import scala.dataflow._


object FPInsertBench extends testing.Benchmark with Utils.Props {
  import Utils._
  
  override def run() {
    val pool = new FlowPool[Data]()
    val builder = new Builder[Data](pool.initBlock)
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
