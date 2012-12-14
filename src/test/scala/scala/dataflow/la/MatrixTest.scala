package scala.dataflow.la

object MatrixTest extends App with ArrayLA with FlowArrayImpl {

  val m = ones(100, 100)
  val v = ones(100)

  println((m.data.partition(100).map(x => x.fold(0.0)(_ + _))).flatten(1).blocking)

}
