package scala.dataflow
package bench

import scala.collection._

object PCReduceBench extends testing.Benchmark with Utils.Props {
  import Utils._
  
  // FIXME how to set parallelism level?
  // collection.parallel.ForkJoinTasks.defaultForkJoinPool.setParallelism(par)
  
  override def run() {
    val pr = (0 until size).par
    val pc = pr map {
      i => new Data(i)
    }
    val rv = pc.aggregate(0)(_ + _.i, _ + _)
    
    println(rv)
  }

}

