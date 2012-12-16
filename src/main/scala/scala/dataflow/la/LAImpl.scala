package scala.dataflow.la

trait LAImpl {

  type Data

  type Matrix
  type Vector
  type Scalar

  implicit def matrix2View(m: Matrix): AbstractMatrix
  implicit def vector2View(v: Vector): AbstractVector
  implicit def scalar2View(s: Scalar): AbstractScalar

  def ones (rows: Int, cols: Int): Matrix
  def zeros(rows: Int, cols: Int): Matrix

  def ones (dim: Int): Vector
  def zeros(dim: Int): Vector

  trait AbstractMatrix {
    def rows: Int
    def cols: Int
    def data: Data
    def t: Matrix
    def +(that: Matrix): Matrix
    def *(that: Vector): Vector
    def *(that: Matrix)(implicit i1: DummyImplicit): Matrix
    def toVector: Vector
  }

  trait AbstractVector {
    def dim: Int
    def data: Data
    def +(that: Vector): Vector
    def *(that: Vector): Scalar
    def toMatrix: Matrix
  }

  trait AbstractScalar {
    def blocking: Double
  }

}
