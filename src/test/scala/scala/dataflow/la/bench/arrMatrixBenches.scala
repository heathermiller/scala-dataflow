package scala.dataflow.la.bench

import scala.dataflow.la._

object FAMatrixBench extends MatrixBench with ArrayLA with FlowArrayImpl
object PAMatrixBench extends MatrixBench with ArrayLA with ParArrayImpl
