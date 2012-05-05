package scala.dataflow

import java.util.concurrent.ConcurrentLinkedQueue

object ConcurrentLinkedQueueBench extends testing.Benchmark with Utils.Props {

  var queue = new ConcurrentLinkedQueue[Int]()

  override def setUp() {
    queue = new ConcurrentLinkedQueue[Int]()
  }

  override def run() {
    for (i <- 1 to size)
      queue.add(0)
  }

  override def tearDown() {}

}
