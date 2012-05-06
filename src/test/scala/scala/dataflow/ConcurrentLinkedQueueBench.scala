package scala.dataflow

import java.lang.Integer
import java.util.concurrent.ConcurrentLinkedQueue

object ConcurrentLinkedQueueBench extends ParInsertBench {

  var queue = new ConcurrentLinkedQueue[Integer]()

  class Inserter(val sz: Int) extends Thread {
    override def run() { for (i <- 1 to sz) queue.add(0) }
  }

  def inserter(sz: Int) = new Inserter(sz)

  override def setUp() {
    queue = new ConcurrentLinkedQueue[Integer]()
  }

}
