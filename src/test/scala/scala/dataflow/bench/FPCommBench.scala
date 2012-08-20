package scala.dataflow.bench

import scala.dataflow._

trait FPCommBench extends testing.Benchmark with Utils.Props with FPBuilder {
  import Utils._
  
  override def run() {
    def start(): Seq[Thread] = {
      val pool = newFP[Data]
      val builder = pool.builder
      val work = size / par
      val data = new Data(0)
      
      val writers = for (ti <- 1 to par) yield task {
        var i = 0
        while (i < work) {
          builder += data
          i += 1
        }
      }
      
      for (x <- pool) {
        // traverse
      }
      
      writers
    }
    
    val writers = start()
    
    writers.foreach(_.join())
  }
  
}
