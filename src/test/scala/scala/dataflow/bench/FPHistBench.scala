package scala.dataflow.bench



import scala.dataflow._
import java.util.concurrent.ThreadLocalRandom



trait FPHistBench extends testing.Benchmark with Utils.Props with FPBuilder {
  import Utils._

  val maxval = 100
  
  override def run() {
    val pool = newFP[Data]
    val builder = pool.builder
    val work = size / par
    val bins = 5 to 20
    def data = new Data(ThreadLocalRandom.current.nextInt(maxval))

    val res = bins.map(s => binning(s,pool))
    
    for (ti <- 1 to par) yield task {
      var i = 0
      while (i < work) {
        builder << data
        i += 1
      }
    }

    builder.seal(size)

    res.foreach(_.blocking)

  }

  private def binning(count: Int, pool: FlowPool[Data]) = {
    val init = Map[Int, Int]()
    val fm = pool.aggregate(init)(mergeMaps _) {
      (acc, x) =>
      val elem  = x.i * count / maxval
      val total = acc.getOrElse(elem, 0) + 1
      acc + (elem -> total)
    } map {
      m => for (i <- 0 to (count - 1)) yield m.getOrElse(i, 0)
    }
    fm
  }

  private def mergeMaps(m1: Map[Int, Int], m2: Map[Int, Int]) = 
    m1 ++ m2.map { case (k, v) => k -> (v + m1.getOrElse(k, 0)) }
  
}

