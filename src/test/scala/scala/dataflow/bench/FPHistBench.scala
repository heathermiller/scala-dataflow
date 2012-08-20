package scala.dataflow.bench



import scala.collection._
import scala.dataflow._
import java.util.concurrent.ThreadLocalRandom



trait FPHistBench extends testing.Benchmark with Utils.Props with FPBuilder {
  import Utils._

  val maxval = 100
  
  override def run() {
    val pool = newFP[Data]
    val builder = pool.builder
    val work = size / par
    val bins = 5 to 15
    def data = new Data(ThreadLocalRandom.current.nextInt(maxval))

    val res = bins.map(s => binning(s, pool))
    
    for (ti <- 1 to par) yield task {
      var i = 0
      while (i < work) {
        builder += data
        i += 1
      }
    }

    builder.seal(size)

    res.foreach(_.blocking)

  }

  private def binning(count: Int, pool: FlowPool[Data]) = {
    def init = new Array[Int](count)
    val fm = pool.aggregate(init)(merge _) {
      (acc, x) =>
      val elem  = x.i * count / maxval
      acc(elem) = acc(elem) + 1
      acc
    }
    fm
  }

  private def merge(a1: Array[Int], a2: Array[Int]) = 
    a1 zip a2 map {
      case (x, y) => x + y
    }
  
}

