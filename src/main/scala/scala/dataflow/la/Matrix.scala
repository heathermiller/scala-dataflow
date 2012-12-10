package scala.dataflow.la

import scala.dataflow.array._

class Matrix(
  val rows: Int,
  val cols: Int,
  val data: FlowArray[Double]
) {

  require(rows * cols == data.size)

  def +(that: Matrix) = {
    require(this.rows == that.rows &&
            this.cols == that.cols)

    new Matrix(rows, cols, (this.data zipMap that.data)(_ + _))
  }

  def *(that: Vector): Vector = {
    require(this.cols == that.dim)

    val res = data.partition(rows) map { x =>
      (x zipMap that.data)(_ * _).fold(0.0)(_ + _)
    } flatten 1

    new Vector(res)
  }

  /*
   * TODO finish implementing
  def *(that: Matrix): Matrix = {
    require(this.m == that.n)
    if (that.m != 1)
      throw new UnsupportedOperationException()

    val res = data.partition(n).map(x => (x zipMap that.data)(_ * _).fold(0.0)(_ + _)).flatten(1)

    new Matrix(this.n, that.m, res)
  }

  def apply(x: Int)(y: Int) = {
    data.blocking(x + y * m)
  }
  */

  def toVector: Vector = {
    if (cols == 1)
      new Vector(data)
    else
      throw new IllegalArgumentException("illegal dimensions")
  }

}

object Matrix {

  def ones(rows: Int, cols: Int) = new Matrix(rows, cols,
                                              FlowArray.tabulate(rows*cols)(x => 1))

}
