package scala.dataflow.api






trait FlowBufferLike[T, Async[X]] extends FlowLike[T] {
  
  /** Analogous to `push`, `enqueue`  
   */
  def <<(x: T): this.type
  
  /** Call this on a `FlowBuffer` to "seal" the interface. So as not to allow it to change any further after `seal`.
   */
  def seal(): FlowBuffer.Blocking[T]

  def sealAfter(n: Int): FlowBuffer.Blocking[T]
  
  def apply(idx: Int)(implicit i: FlowBuffer.Index[Async]): T
  
  def isEmpty(implicit e: FlowBuffer.IsEmpty[Async]): Async[Boolean]
  
  def foreach[U](f: T => U)(implicit e: FlowBuffer.Foreach[Async]): Async[Unit]
  
  def blocking: FlowReader.Blocking[T]
  
}


trait FlowBuffer[T] extends FlowBufferLike[T, Future]


object FlowBuffer extends FlowFactory[FlowBufferLike] {
  
  def apply[T](): FlowBuffer[T] = null
  
  /* type classes */
  
  trait Index[Async[X]] {
    def apply[T](coll: FlowBufferLike[T, Async], idx: Int): Async[T]
  }
  
  implicit object FutureIndex extends Index[Future] {
    def apply[T](coll: FlowBufferLike[T, Future], idx: Int) = null
  }
  
  implicit object IdIndex extends Index[Id] {
    def apply[T](coll: FlowBufferLike[T, Id], idx: Int) = throw new Exception
  }
  
  trait IsEmpty[Async[X]] {
    def apply[T](coll: FlowBufferLike[T, Async]): Async[Boolean]
  }
  
  implicit object FutureIsEmpty extends IsEmpty[Future] {
    def apply[T](coll: FlowBufferLike[T, Future]): Future[Boolean] = null
  }
  
  implicit object IdIsEmpty extends IsEmpty[Id] {
    def apply[T](coll: FlowBufferLike[T, Id]): Boolean = throw new Exception
  }
  
  trait Foreach[Async[X]] {
    def apply[T, U](coll: FlowBufferLike[T, Async], f: T => U): Async[Boolean]
  }
  
  implicit object FutureForeach extends Foreach[Future] {
    def apply[T, U](coll: FlowBufferLike[T, Future], f: T => U) = null
  }
  
  implicit object IdForeach extends Foreach[Id] {
    def apply[T, U](coll: FlowBufferLike[T, Id], f: T => U) = throw new Exception
  }
  
}


