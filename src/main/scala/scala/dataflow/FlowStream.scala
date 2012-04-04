package scala.dataflow





trait FlowStreamLike[T, Async[X]] extends FlowLike[T] {
}


trait FlowStream[T] extends FlowStreamLike[T, Future] {
  
  def <<(elem: T): FlowStream[T]
  
}


class Head[T]() extends FlowStream[T] {
  
  var actual: FlowStream[T] = null
  
  def <<(elem: T) = null
  
  def onBind[U](body: T => U) = null
  
  def reader = null
  
}


class <<[T](val head: T) extends FlowStream[T] {
  
  def <<(elem: T) = null
  
  def onBind[U](body: T => U) = null
  
  def reader = null
  
}


object << {
  
  def unapply[T](fs: FlowStream[T]): Option[(T, FlowStream[T])] = {
    None
  }
  
}


object Seal extends FlowStream[Nothing] {
  
  def <<(elem: Nothing): FlowStream[Nothing] = throw new UnsupportedOperationException
  
  def onBind[U](body: Nothing => U) = throw new UnsupportedOperationException
  
  def reader = FlowReader.Empty
  
}


object FlowStream extends FlowFactory[FlowStreamLike] {
  
  def apply[T](): FlowStream[T] = null
  
}
