package scala.dataflow.bench

import scala.dataflow.Utils
import java.util.concurrent.BlockingQueue
import scala.util.Random

trait QHistBench extends testing.Benchmark with Utils.Props with BQBuilder {
  import Utils._

  val maxval = 100

  class Binner(queue: BlockingQueue[Data], work: Int, bins: Int) extends Thread {
    val acc = Array.fill[Int](bins)(0)
    override def run() = {
      var i = 0

      while (i < work) {
        val v = queue.take().i
        val pos = v * bins / maxval
        acc(pos) = acc(pos) + 1
        i += 1
      }    
    }
  }

  override def run() {
    val work = size
    val bins = 5 to 20
    val qs = bins.map(ign => newQ[Data])

    def data = new Data(Random.nextInt(maxval))
    var i = 0
    var agg = 0

    val binners = bins.zip(qs).map {
      case (c,q) => new Binner(q, work, c)
    }

    binners.foreach(_.start())

    while (i < work) {
      val v = data
      qs.foreach(_.add(v))
      i = i + 1
    }

    binners.foreach(_.join())

  }
  
}
