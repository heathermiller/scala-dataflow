package scala.dataflow.bench

import scala.dataflow.Utils
import java.util.Queue

trait QReduceBench extends testing.Benchmark with Utils.Props with BQBuilder {
  import Utils._

  class Inserter(queue: Queue[Data], work: Int) extends Thread {
    override def run() = {
      val data = new Data(0)
      var i = 0

      while (i < work) {
        queue.add(data)
        i += 1
      }    
    }
  }

  override def run() {
    val queue = newQ[Data]
    val work = size
    var i = 0
    var agg = 0

    val ins = new Inserter(queue, work)
    ins.start()

    while (i < work) {
      agg += queue.take().i
      i = i + 1
    }

    // For completeness
    ins.join()

  }
  
}
