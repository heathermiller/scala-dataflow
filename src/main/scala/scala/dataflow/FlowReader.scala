package scala.dataflow





/** Basically an iterator. Possible to get it out of different flow collections. Basically describe a sequence of incoming elements.
 */
trait FlowReaderLike[T, Async[X]] {
  
  /** Look at the next element, but don't take it from the next `FlowReader`
   *  Useful if you have 2 incoming sorted buffer and you want to merge them
   *  to create a single sorted buffer. `peek` would allow you to check the head of both and determine which to actually `pop` (take)
   */
  def peek: Async[T]
  
  /** Analogous to the `next` method on iterators.
   */
  def pop(): Async[T]
  
  def isEmpty: Async[Boolean]
  
  def foreach[U](f: T => U): Async[Unit]
  
  /** 
   */
  def blocking: FlowReader.Blocking[T]
  
}


trait FlowReader[T] extends FlowReaderLike[T, Future]


object FlowReader extends FlowFactory[FlowReaderLike] {
  
  object Empty extends FlowReader[Nothing] {
    def peek = throw new NoSuchElementException("Empty.peek")
    def pop() = throw new NoSuchElementException("Empty.peek")
    def isEmpty = null
    def foreach[U](f: Nothing => U) = null
    def blocking = null
  }
  
}
