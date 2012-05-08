package scala.dataflow

import java.lang.Integer
import java.util.concurrent.ConcurrentLinkedQueue

object ConcurrentLinkedQueueBench extends ParInsertBench {
  import Utils._
  
  var queue = new ConcurrentLinkedQueue[Data]()
  val data = new Data(0)
  
  class Inserter(val sz: Int) extends Thread {
    override def run() {
      var i = 0
      val until = sz
      while (i < until) {
        queue.add(data)
        i += 1
      }
    }
  }

  def inserter(sz: Int) = new Inserter(sz)

  override def setUp() {
    queue = new ConcurrentLinkedQueue[Data]()
  }

}
