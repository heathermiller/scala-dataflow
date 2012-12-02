package scala.dataflow.la

class Matrix(
  val n: Int,
  val m: Int,
  val data: FlowArray[Double]
) {

  require(n * m == data.size)

  def +(that: Matrix[Double]) = {
    require(this.n == that.n && this.m == that.m)
    new Matrix(n,m, (this.data zipMap that.data)(_ + _))
  }

  def *(that: Matrix[Double]) = {
    require(this.n == that.m)
    if (that.m != 1)
      throw new UnsupportedOperationException()

    data.partition(n).flatMapN(1)(x => (x zipMap that)(_ * _).fold(0)(_ + _))
  }

}
