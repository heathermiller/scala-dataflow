package scala.dataflow







trait FlowArrayLike[I, T, Async[X]] extends FlowLike[(I, T)] {

  def dim: I

  def <<(iv: (I, T))(implicit a: FlowArray.Put[I]): this.type

  def apply(i: I)(implicit a: FlowArray.Apply[I, Async]): Async[T]

  def foreach[U](f: T => U)(implicit e: FlowArray.Foreach[I,Async]): Async[Unit]

  def zipWithIndex: FlowArrayLike[I, (I, T), Async]

  def blocking: FlowArray.Blocking[I,T]

}


trait FlowArray[I, T] extends FlowArrayLike[I, T, Future]


object FlowArray extends FlowMapFactory[FlowArrayLike] {

  def apply[T](n1: Int): FlowArray[Int, T] = null
  def apply[T](n1: Int, n2: Int): FlowArray[(Int, Int), T] = null

  /* type classes */

  trait Apply[I, Async[X]] {
    def apply[T](coll: FlowArrayLike[I, T, Async], i: I): Async[T]
  }

  implicit object FutureApply1D extends Apply[Int,Future] {
    def apply[T](coll: FlowArrayLike[Int, T, Future], i: Int): Future[T] = null
  }

  implicit object IdApply1D extends Apply[Int,Id] {
    def apply[T](coll: FlowArrayLike[Int, T, Id], i: Int): T = throw new Exception
  }

  implicit object FutureApply2D extends Apply[(Int,Int),Future] {
    def apply[T](coll: FlowArrayLike[(Int, Int), T, Future], i: (Int,Int)): Future[T] = null
  }

  implicit object IdApply2D extends Apply[(Int,Int),Id] {
    def apply[T](coll: FlowArrayLike[(Int, Int), T, Id], i: (Int,Int)): T = throw new Exception
  }

  trait Put[I] {
    def <<[T, Async[X]](coll: FlowArrayLike[I, T, Async], iv: (I,T))
  }

  implicit object Put1D extends Put[Int] {
    def <<[T, Async[X]](coll: FlowArrayLike[Int, T, Async], iv: (Int,T)) {}
  }

  implicit object Put2D extends Put[(Int,Int)] {
    def <<[T, Async[X]](coll: FlowArrayLike[(Int, Int), T, Async], iv: ((Int, Int),T)) {}
  }

  trait Foreach[I, Async[X]] {
    def apply[T, U](coll: FlowArrayLike[I, T, Async], f: T => U): Async[Unit]
  }

  implicit object FutureForeach extends Foreach[Int, Future] {
    def apply[T, U](coll: FlowArrayLike[Int, T, Future], f: T => U): Future[Unit] = null
  }

}

