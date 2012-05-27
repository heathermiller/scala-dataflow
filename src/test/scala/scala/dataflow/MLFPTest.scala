package scala.dataflow

import java.lang.Thread
import java.util.concurrent.ThreadLocalRandom

object MLFPTest extends App {

  val tasks = 100
  val n = 1000
  val pool = new MultiLaneFlowPool[(Int,Int)](tasks/2)
  val b = pool.builder

  val r = pool.mappedFold(0)(_ + _)(x => x._2)
  val s = pool.mappedFold(0)(_ + _)(x => x._1)

  b.seal(n * tasks)

  for (i <- 1 to tasks) {
    task {
      val v = ThreadLocalRandom.current()
      for (j <- 1 to n) {
        b << (i,j)
        Thread.sleep(v.nextLong(10))
      }
    }
  }

  println("Done: %d, %d".format(s.blocking._2, r.blocking._2))

  def task(f: => Unit) {
    new Thread(new Runnable() { def run() = f }).start()
  }

}
