package scala.dataflow






trait FlowArrayLike[T, Async[X]] extends FlowLike[T] {

  def length: Int

  def <<(i: Int, x: T): this.type

  def apply(i: Int)(implicit a: FlowArray.Apply[Async]): Async[T]

  def blocking: FlowArray.Blocking[T]

}


trait FlowArray[T] extends FlowVarLike[T, Future]


object FlowArray extends FlowFactory[FlowArrayLike] {

  def apply[T](size: Int): FlowArray[T] = null

  /* type classes */

  trait Apply[Async[X]] {
    def apply[T](coll: FlowArrayLike[T, Async], i: Int): Async[T]
  }

  implicit object FutureApply extends Apply[Future] {
    def apply[T](coll: FlowArrayLike[T, Future], i: Int): Future[T] = null
  }

  implicit object IdApply extends Apply[Id] {
    def apply[T](coll: FlowArrayLike[T, Id], i: Int): T = throw new Exception
  }

}
