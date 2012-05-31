package scala.dataflow.bench

import scala.dataflow.Utils
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ThreadLocalRandom

trait QHistBench extends testing.Benchmark with Utils.Props with BQBuilder {
  import Utils._

  val maxval = 100

  override def run() {
    val work = size / par
    val bins = 5 to 15
    val qs = bins.map(ign => newQ[Data])
    def data = new Data(ThreadLocalRandom.current.nextInt(maxval))

    val binners = for ((cnt, q) <- bins zip qs) yield task {
      val acc = Array.fill[Int](cnt)(0)
      var i = 0

      while (i < size) {
        val v = q.take().i
        val pos = v * cnt / maxval
        acc(pos) = acc(pos) + 1
        i += 1
      }    
    }

    val writers = for (ti <- 1 to par) yield task {
      var i = 0
      while (i < work) {
        val v = data
        qs.foreach(_.add(v))
        i += 1      
      }
    }

    binners.foreach(_.join())
    writers.foreach(_.join())

  }
  
}
