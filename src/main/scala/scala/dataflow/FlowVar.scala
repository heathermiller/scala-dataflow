package scala.dataflow






trait FlowVarLike[T, Async[X]] extends FlowLike[T] {
  
  def <<(x: T): FlowVar.Blocking[T]
  
  def apply()(implicit a: FlowVar.Apply[Async]): Async[T]
  
  def blocking: FlowVar.Blocking[T]
  
}


trait FlowVar[T] extends FlowVarLike[T, Future]


object FlowVar extends FlowFactory[FlowVarLike] {
  
  def apply[T](): FlowVar[T] = null
  
  /* type classes */
  
  trait Apply[Async[X]] {
    def apply[T](coll: FlowVarLike[T, Async]): Async[T]
  }
  
  implicit object FutureApply extends Apply[Future] {
    def apply[T](coll: FlowVarLike[T, Future]): Future[T] = null
  }
  
  implicit object IdApply extends Apply[Id] {
    def apply[T](coll: FlowVarLike[T, Id]): T = throw new Exception
  }
  
}


