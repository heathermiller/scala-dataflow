package scala.dataflow






trait FlowBufferLike[T, Async[X]] extends FlowLike[T] {
  
  /**
  * Analogous to `push`, `enqueue`  
  */
  def <<(x: T): this.type
  
  /**
  * Call this on a `FlowBuffer` to "seal" the interface. So as not to allow it to change any further after `seal`.
  */
  def seal(): FlowBuffer.Blocking[T]
  
  def isEmpty: Async[Boolean]
  
  def foreach[U](f: T => U): Async[Unit]
  
  def blocking: FlowReader.Blocking[T]
  
  def map[S](f: T => S) = {
    val fb = FlowBuffer[T]()
    this foreach { fb << _ } andThen { fb.seal() }
    fb
  }
  
}


trait FlowBuffer[T] extends FlowBufferLike[T, Future]


object FlowBuffer extends FlowFactory[FlowBufferLike] {
  
  def apply[T](): FlowBuffer[T] = null
  
}
