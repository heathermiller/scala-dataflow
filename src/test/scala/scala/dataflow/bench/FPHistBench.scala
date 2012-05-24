package scala.dataflow.bench

import scala.dataflow._
import scala.util.Random

trait FPHistBench extends testing.Benchmark with Utils.Props with FPBuilder {
  import Utils._

  val maxval = 100
  
  override def run() {
    val pool = newFP[Data]
    val builder = pool.builder
    val work = size
    val bins = 5 to 20
    def data = new Data(Random.nextInt(maxval))
    var i = 0

    val res = bins.map(s => binning(s,pool))
    
    while (i < work) {
      builder << data
      i += 1
    }

    builder.seal(work)

    res.foreach(_.blocking)

  }

  private def binning(count: Int, pool: FlowPool[Data]) = {
    val init = Map[Int,Int]()
    val fm = pool.mappedFold(init)(mergeMaps _)(x => Map(x.i * count / maxval -> 1))
    fm.map {
      case (cnt,m) => for (i <- 0 to (count - 1)) yield m.getOrElse(i,0)
    }
  }

  private def mergeMaps(m1: Map[Int,Int], m2: Map[Int,Int]) = 
    m1 ++ m2.map { case (k,v) => k -> (v + m1.getOrElse(k,0)) }
  
}
