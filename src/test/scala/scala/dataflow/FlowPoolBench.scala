package scala.dataflow

import java.lang.Integer

object FlowPoolBench extends ParInsertBench {

  var pool = new impl.FlowPool[Integer]()

  class Inserter(val sz: Int) extends Thread {
    val build = pool.builder
    override def run() { for (i <- 1 to sz)  build << 0 }
  }

  def inserter(sz: Int) = new Inserter(sz)

  override def setUp() {
    pool = new impl.FlowPool[Integer]()
  }

}
