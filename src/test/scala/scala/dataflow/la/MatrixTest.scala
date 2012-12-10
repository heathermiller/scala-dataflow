package scala.dataflow.la

import scala.dataflow.array._

object MatrixTest extends App {

  val m = Matrix.ones(100, 100)
  val v = Vector.ones(100)

  println((m.data.partition(100).map(x => x.fold(0.0)(_ + _))).flatten(1).blocking)

}
