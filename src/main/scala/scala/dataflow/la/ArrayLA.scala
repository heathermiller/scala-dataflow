package scala.dataflow.la

trait ArrayLA extends LAImpl {
  this: ArrayImpl =>

  type Data = Array[Double]
  type Scalar = FoldResult[Double]

  def ones (rows: Int, cols: Int) =
    new Matrix(rows, cols, tabulate(rows*cols)(x => 1.0))
  def zeros(rows: Int, cols: Int) =
    new Matrix(rows, cols, tabulate(rows*cols)(x => 0.0))

  def ones (dim: Int) = new Vector(tabulate(dim)(x => 1.0))
  def zeros(dim: Int) = new Vector(tabulate(dim)(x => 0.0))

  implicit def scalar2View(r: Scalar) = new BoxedScalar(r)
  class BoxedScalar(r: FoldResult[Double]) extends AbstractScalar {
    def blocking = foldResult2View(r).blocking
  }
 
  class Matrix(
    val rows: Int,
    val cols: Int,
    val data: Data
  ) extends AbstractMatrix {

    require(rows * cols == data.size)

    def t = new Matrix(cols, rows, this.data.transpose(cols))

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

    def *(that: Matrix)(implicit i1: DummyImplicit): Matrix = {
      require(this.cols == that.rows)

      val mX = data.partition(this.rows)
      val mY = that.t.data.partition(that.cols)

      val res = mX.flatMapN(that.cols) { vX =>
        mY.map(vY => (vX zipMap vY)(_ * _).fold(0.0)(_ + _)).flatten(1)
      }

      new Matrix(this.rows, that.cols, res)
    }

    def toVector: Vector = {
      if (cols == 1)
        new Vector(data)
      else
        throw new IllegalArgumentException("illegal dimensions")
    }

  }

  class Vector(
    val data: Data
  ) extends AbstractVector {

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

  implicit def vector2View(v: Vector) = v
  implicit def matrix2View(v: Matrix) = v

}
