package scala.dataflow.bench

import scala.dataflow._


object FPReduceBench extends testing.Benchmark with Utils.Props {
  import Utils._
  
  override def run() {
    val pool = new FlowPool[Data]()
    val builder = new Builder[Data](pool.initBlock)
    val work = size
    val data = new Data(0)
    var i = 0

    val res = pool.mappedFold(0)(_ + _)(_.i)
    
    while (i < work) {
      builder << data
      i += 1
    }

    builder.seal(work)

    res.blocking

  }
  
}
