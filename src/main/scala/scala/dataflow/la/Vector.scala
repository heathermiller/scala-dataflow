package scala.dataflow.la

import scala.dataflow.array._

class Vector(
  val data: FlowArray[Double]
) {

  def dim = data.size

  def +(that: Vector) = {
    require(this.dim == that.dim)
    new Vector((this.data zipMap that.data)(_ + _))
  }

  def *(that: Vector) = {
    require(this.dim == that.dim)
    (this.data zipMap that.data)(_ * _).fold(0.0)(_ + _)
  }

  def toMatrix = new Matrix(dim, 1, data)

}

object Vector {

  def ones(dim: Int) = new Vector(FlowArray.tabulate(dim)(x => 1))

}
