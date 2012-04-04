package scala.dataflow






trait FlowReaderLike[T, Async[X]] {
  
  def peek: Async[T]
  
  def pop(): Async[T]
  
  def isEmpty: Boolean
  
  def blocking: FlowReader.Blocking[T]
  
}


trait FlowReader[T] extends FlowReaderLike[T, Future]


object FlowReader extends FlowFactory[FlowReaderLike] {
}
