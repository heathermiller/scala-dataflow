package scala.dataflow






trait FlowQueueLike[T, Async[X]] extends FlowLike[T] {
  
  def <<(x: T): this.type
  
  def seal(): FlowQueue.Blocking[T]
  
  def isEmpty: Async[Boolean]
  
  def foreach[U](f: T => U): Async[Unit]
  
  def blocking: FlowReader.Blocking[T]
  
}


trait FlowQueue[T] extends FlowQueueLike[T, Future]


object FlowQueue extends FlowFactory[FlowQueueLike] {
  
  def apply[T](): FlowQueue[T] = null
  
}
