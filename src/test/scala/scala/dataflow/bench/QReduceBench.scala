package scala.dataflow.bench

import scala.dataflow.Utils

// TODO should we parallelize the aggregation? How?
trait QReduceBench extends testing.Benchmark with Utils.Props with BQBuilder {
  import Utils._

  override def run() {
    val queue = newQ[Data]
    val work = size / par
    var i = 0
    var agg = 0

    val writers = for (ti <- 1 to par) yield task {
      val data = new Data(0)
      var i = 0
      while (i < work) {
        queue.add(data)
        i += 1
      }
    }

    // TODO par readers
    while (i < size) {
      agg += queue.take().i
      i = i + 1
    }

    writers.foreach(_.join())

  }
  
}
