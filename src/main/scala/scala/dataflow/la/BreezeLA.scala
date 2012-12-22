package scala.dataflow.la

import breeze.linalg._

trait BreezeLA extends LAImpl {

  type Data   = scala.Array[Double]
  type Vector = DenseVector[Double]
  type Matrix = DenseMatrix[Double]
  type Scalar = Double

  def ones (rows: Int, cols: Int) = DenseMatrix.fill(rows, cols)(1.0)
  def zeros(rows: Int, cols: Int) = DenseMatrix.zeros[Double](rows, cols)

  def ones (dim: Int) = DenseVector.fill(dim)(1.0)
  def zeros(dim: Int) = DenseVector.zeros[Double](dim)

  implicit def scalar2View(r: Scalar) = new BoxedScalar(r)
  class BoxedScalar(r: Double) extends AbstractScalar {
    def blocking = r
  }

  implicit def matrix2View(v: Matrix) = new BoxedMatrix(v) 
  class BoxedMatrix(m: Matrix) extends AbstractMatrix {
    def rows = m.rows
    def cols = m.cols
    def t = m.t
    def +(that: Matrix) = m + that
    def *(that: Vector): Vector = m * that
    def *(that: Matrix)(implicit i1: DummyImplicit): Matrix =
      m * that

    def toVector = {
      if (cols == 1)
        new DenseVector(m(::,1).data)
      else
        throw new IllegalArgumentException("illegal dimensions")
    }
    def data = m.data
  }

  implicit def vector2View(v: Vector) = new BoxedVector(v)
  class BoxedVector(v: Vector) extends AbstractVector {
    def dim = v.length
    def +(that: Vector) = v + that
    def *(that: Vector) = v dot that
    def toMatrix = new DenseMatrix(dim, data, 0)
    def data = v.data
  }

}
