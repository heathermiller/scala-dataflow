package scala.dataflow



import java.util.concurrent.LinkedTransferQueue



object LTQBench extends testing.Benchmark with Utils.Props {
  import Utils._
  
  
  def run() {
    val queue = new LinkedTransferQueue[Data]()
    test(queue)
  }
  
  private def test(queue: LinkedTransferQueue[Data]) {
    val data = new Data(0)
    var i = 0
    val until = size
    
    while (i < until) {
      queue.add(data)
      i += 1
    }
  }

}
