package scala.dataflow




object FlowPoolBench extends ParInsertBench {
  import Utils._
  
  val data = new Data(0)
  var pool = new impl.FlowPool[Data]()
  
  class Inserter(val sz: Int) extends Thread {
    val build = pool.builder
    override def run() {
      var i = 0
      while (i < sz) {
        build << data
        i += 1
      }
    }
  }
  
  def inserter(sz: Int) = new Inserter(sz)
  
  override def setUp() {
    pool = new impl.FlowPool[Data]()
  }
  
}
