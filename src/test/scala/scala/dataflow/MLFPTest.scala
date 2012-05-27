package scala.dataflow

import java.lang.Thread
import java.util.concurrent.ThreadLocalRandom
import scala.collection.mutable.{ Set => MSet }

object MLFPTest extends App {

  val tasks = 50
  val n = 1000
  val pool = new MultiLaneFlowPool[(Int,Int)](tasks/2)
  val b = pool.builder

  val vals = MSet.empty[(Int,Int)]

  val sfill = pool.doForAll(vals += _)
  val rf = pool.mappedFold(0)(_ + _)(x => x._2)
  val sf = pool.mappedFold(0)(_ + _)(x => x._1)

  val rc = n * (n + 1) / 2 * tasks
  val sc = tasks * (tasks + 1) * n

  for (i <- 1 to tasks) {
    task {
      val v = ThreadLocalRandom.current()
      for (j <- 1 to n) {
        b << (i,j)
        Thread.sleep(v.nextLong(10))
      }
    }
  }

  b.seal(n * tasks)

  val rv = rf.blocking._2
  val sv = sf.blocking._2
  sfill.blocking

  val vals_should = ( for (i <- 1 to tasks ; j <- 1 to n) yield (i,j) ) toSet

  val missing = vals_should.diff(vals).size

  println("%d values missing (diff)".format(missing))
  println("%d values missing (sub)".format(vals_should.size - vals.size))
  println("Done: r: %d (should: %d), s: %d (should: %d)".format(rv,rc,sv,sc))

  def task(f: => Unit) {
    new Thread(new Runnable() { def run() = f }).start()
  }

}
