package scala.dataflow






trait FlowArrayLike[I, T, Async[X]] extends FlowLike[(I, T)] {

  def dim: I

  def <<(iv: (I, T))(implicit a: FlowArray.Put[I]): this.type

  def apply(i: I)(implicit a: FlowArray.Apply[I, Async]): Async[T]

  def foreach[U](f: T => U)(implicit e: FlowArray.Foreach[Async]): Async[Unit]

  def zipWithIndex: FlowArrayLike[I, (I, T), Async]

  def blocking: FlowArray.Blocking[T]

}


trait FlowArray[I, T] extends FlowVarLike[I, T, Future]


object FlowArray extends FlowFactory[FlowArrayLike] {

  def apply[T](n1: Int): FlowArray[Int, T] = null
  def apply[T](n1: Int, n2: Int): FlowArray[(Int, Int), T] = null

  /* type classes */

  trait Apply[I, Async[X]] {
    def apply[T](coll: FlowArrayLike[I, T, Async], i: I): Async[T]
  }

  implicit object 1DFutureApply extends Apply[Int,Future] {
    def apply[T](coll: FlowArrayLike[I, T, Future], i: Int): Future[T] = null
  }

  implicit object 1DIdApply extends Apply[Int,Id] {
    def apply[T](coll: FlowArrayLike[I, T, Id], i: Int): T = throw new Exception
  }

  trait Put[I] {
    def <<[T, Async](coll: FlowArrayLike[I, T, Async], iv: (I,T))
  }

  implicit object 1DPut extends Put[Int] {
    def <<[T, Async](coll: FlowArrayLike[Int, T, Async], iv: (Int,T))
  }

  implicit object 2DPut extends Put[(Int,Int)] {
    def <<[T, Async](coll: FlowArrayLike[(Int, Int), T, Async], iv: ((Int, Int),T))
  }

  trait Foreach[Async[X]] {
    def apply[T, U](coll: FlowArrayLike[T, Async], f: T => U): Async[Boolean]
  }

}
