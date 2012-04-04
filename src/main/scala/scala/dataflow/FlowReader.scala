package scala.dataflow






trait FlowReaderLike[T, Async[X]] {
  
  def peek: Async[T]
  
  def pop(): Async[T]
  
  def isEmpty: Boolean
  
  def foreach[U](f: T => U): Async[Unit]
  
  def blocking: FlowReader.Blocking[T]
  
}


trait FlowReader[T] extends FlowReaderLike[T, Future]


object FlowReader extends FlowFactory[FlowReaderLike] {
}
