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


object CLQBench extends testing.Benchmark with Utils.Props {
  import Utils._
  
  
  def run() {
    val queue = new ConcurrentLinkedQueue[Data]()
    test(queue)
  }
  
  private def test(queue: ConcurrentLinkedQueue[Data]) {
    val data = new Data(0)
    var i = 0
    val until = size
    
    while (i < until) {
      queue.add(data)
      i += 1
    }
  }

}
