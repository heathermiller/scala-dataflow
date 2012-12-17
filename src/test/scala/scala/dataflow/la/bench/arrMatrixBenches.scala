package scala.dataflow.la.bench

import scala.dataflow.la._
import scala.dataflow.array._

object FAMatrixBench extends MatrixBench with ArrayLA with FlowArrayImpl {
  override def tearDown {
    FAJob.printStats()
    FAJob.resetStats()
  }
}
object PAMatrixBench extends MatrixBench with ArrayLA with ParArrayImpl
