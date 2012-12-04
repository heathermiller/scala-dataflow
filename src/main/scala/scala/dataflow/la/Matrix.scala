package scala.dataflow.la

import scala.dataflow.array._

class Matrix(
  val n: Int,
  val m: Int,
  val data: FlowArray[Double]
) {

  require(n * m == data.size)

  def +(that: Matrix) = {
    require(this.n == that.n && this.m == that.m)
    new Matrix(n,m, (this.data zipMap that.data)(_ + _))
  }

  def *(that: Matrix) = {
    require(this.n == that.m)
    if (that.m != 1)
      throw new UnsupportedOperationException()

    data.partition(n).map(x => (x zipMap that.data)(_ * _).fold(0.0)(_ + _)).flatten(1)
  }

}
