package scala.dataflow.bench

import scala.dataflow._

trait FPBuilder { def newFP[T]: FlowPool[T] }
trait SLFPBuilder {
  def newFP[T] = new SingleLaneFlowPool[T]()
}
