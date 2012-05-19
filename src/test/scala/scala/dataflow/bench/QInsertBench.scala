package scala.dataflow.bench

import scala.dataflow.Utils

trait QInsertBench extends testing.Benchmark with Utils.Props with QBuilder {
  import Utils._

  override def run() {
    val queue = newQ[Data]
    val work = size
    val data = new Data(0)
    var i = 0

    while (i < work) {
      queue.add(data)
      i += 1
    }    

  }
  
}
