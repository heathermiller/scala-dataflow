package scala.dataflow






trait FlowBufferLike[T, Async[X]] extends FlowLike[T] {
  
  def <<(x: T): this.type
  
  def seal(): FlowBuffer.Blocking[T]
  
  def isEmpty: Async[Boolean]
  
  def foreach[U](f: T => U): Async[Unit]
  
  def blocking: FlowReader.Blocking[T]
  
}


trait FlowBuffer[T] extends FlowBufferLike[T, Future]


object FlowBuffer extends FlowFactory[FlowBufferLike] {
  
  def apply[T](): FlowBuffer[T] = null
  
}
