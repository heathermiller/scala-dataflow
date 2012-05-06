package scala.dataflow

import java.util.concurrent.ConcurrentLinkedQueue

abstract class ParInsertBench extends testing.Benchmark with Utils.Props {

  def inserter(sz: Int): Thread

  override def run() {
    val work = size / par
    val threads = Array.fill(par)(inserter(work))
    threads.foreach(_.start())
    threads.foreach(_.join())
  }

}
