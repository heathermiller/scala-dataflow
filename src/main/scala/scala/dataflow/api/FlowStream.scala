package scala.dataflow.api





trait FlowStreamLike[T, Async[X]] extends FlowLike[T] {
  
  def isEmpty(implicit e: FlowStream.IsEmpty[Async]): Async[Boolean]
  
  def blocking: FlowStream.Blocking[T]
  
  def async: FlowStream[T] = null
  
  def onReady[U](body: FlowStream.Blocking[T] => U): Unit = null
  
  def <<(elem: T): this.type
  
}


trait FlowStream[T] extends FlowStreamLike[T, Future] {
  
}


object FlowStream extends FlowFactory[FlowStreamLike] {
  
  def apply[T](): FlowStream[T] = null
  
  /* typeclass */
  
  trait IsEmpty[Async[X]] {
    def apply[T](coll: FlowBufferLike[T, Async]): Async[Boolean]
  }
  
  implicit object FutureIsEmpty extends IsEmpty[Future] {
    def apply[T](coll: FlowBufferLike[T, Future]): Future[Boolean] = null
  }
  
  implicit object IdIsEmpty extends IsEmpty[Id] {
    def apply[T](coll: FlowBufferLike[T, Id]): Boolean = throw new Exception
  }  
  
}


class Head[T]() extends FlowStream[T] {
  
  var actual: FlowStream[T] = null
  
  def <<(elem: T) = null
  
  def onBind[U](body: T => U) = null
  
  def reader = null
  
  def isEmpty(implicit e: FlowStream.IsEmpty[Future]) = throw new Exception
  
  def barrier = this
  
  def blocking = null
}


class <<[T](val head: T) extends FlowStream[T] {
  
  def <<(elem: T) = null
  
  def onBind[U](body: T => U) = null
  
  def reader = null
  
  def isEmpty(implicit e: FlowStream.IsEmpty[Future]) = null
  
  def barrier = this
  
  def blocking = null
}


object << {
  
  def unapply[T](fs: FlowStream.Blocking[T]): Option[(T, FlowStream[T])] = fs.asInstanceOf[FlowStream[T]] match {
    case head: Head[_] => null
    case bind: <<[_] => Some((bind.head, null))
    case seal: Seal => None
  }
  
}


class Seal extends FlowStream[Nothing] {
  
  def <<(elem: Nothing): this.type = throw new UnsupportedOperationException
  
  def onBind[U](body: Nothing => U) = throw new UnsupportedOperationException
  
  def reader = FlowReader.empty
  
  def isEmpty(implicit e: FlowStream.IsEmpty[Future]) = null
  
  def barrier = this
  
  def blocking = null
}


object Seal {
  
  def unapply[T](fs: FlowStream.Blocking[T]): Boolean = fs.asInstanceOf[FlowStream[T]] match {
    case head: Head[_] => false
    case bind: <<[_] => false
    case seal: Seal => true
  }
  
}
