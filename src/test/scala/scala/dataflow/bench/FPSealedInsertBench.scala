package scala.dataflow.bench

import scala.dataflow._

object FPSealedInsertBench extends testing.Benchmark with Utils.Props {
  import Utils._
  
  override def run() {
    val pool = new FlowPool[Data]()
    val builder = new Builder[Data](pool.initBlock)
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

