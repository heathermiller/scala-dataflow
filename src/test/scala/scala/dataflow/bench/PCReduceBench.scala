package scala.dataflow
package bench

import scala.collection._

object PCReduceBench extends testing.Benchmark with Utils.Props {
  import Utils._
  
  //collection.parallel.ForkJoinTasks.defaultForkJoinPool.setParallelism(par)
  val fjtasksupport = new parallel.ForkJoinTaskSupport(new scala.concurrent.forkjoin.ForkJoinPool(2))

  override def run() {
    val pr = (0 until size).par
    pr.tasksupport = fjtasksupport
    val pc = pr map {
      i => new Data(i)
    }
    val rv = pc.aggregate(0)(_ + _.i, _ + _)
    
    println(rv)
  }

}

