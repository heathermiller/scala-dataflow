package scala.dataflow

import java.lang.Integer

object FlowPoolBench extends testing.Benchmark with Utils.Props {

  var pool = new impl.FlowPool[Integer]()

  override def setUp() {
    pool = new impl.FlowPool[Integer]()
  }

  override def run() {
    val build = pool.builder
    for (i <- 1 to size)
      build << 0
  }

  override def tearDown() {}

}
