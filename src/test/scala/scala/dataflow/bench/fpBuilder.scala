package scala.dataflow.bench



import scala.dataflow._
import pool._



trait FPBuilder { def newFP[T]: FlowPool[T] }


trait SLFPBuilder {
  def newFP[T] = new pool.Linear[T]()
}


trait MLFPBuilder extends Utils.Props {
  def newFP[T] = new pool.MultiLane[T](lanes)
}

