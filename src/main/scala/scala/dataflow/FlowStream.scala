package scala.dataflow





trait FlowStreamLike[T, Async[X]] extends FlowLike[T] {
  
  def isEmpty: Async[Boolean]
  
  def blocking: FlowStream.Blocking[T]
  
  def async: FlowStream[T] = null
  
  def onReady[U](body: FlowStream.Blocking[T] => U): Unit = null
  
  def <<(elem: T): this.type
  
}


trait FlowStream[T] extends FlowStreamLike[T, Future] {
  
}


class Head[T]() extends FlowStream[T] {
  
  var actual: FlowStream[T] = null
  
  def <<(elem: T) = null
  
  def onBind[U](body: T => U) = null
  
  def reader = null
  
  def isEmpty = throw new Exception
  
  def barrier = this
  
  def blocking = null
}


class <<[T](val head: T) extends FlowStream[T] {
  
  def <<(elem: T) = null
  
  def onBind[U](body: T => U) = null
  
  def reader = null
  
  def isEmpty = null
  
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
  
  def reader = FlowReader.Empty
  
  def isEmpty = null
  
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


object FlowStream extends FlowFactory[FlowStreamLike] {
  
  def apply[T](): FlowStream[T] = null
  
}
