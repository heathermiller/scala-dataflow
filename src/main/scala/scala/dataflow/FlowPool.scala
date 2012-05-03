package scala.dataflow

trait FlowPoolLike[T, Async[X]] extends FlowLike[T] {

  def builder: FlowPool.Builder[T]

  def foreach[U](f: T => U): Unit

  // TODO do we want to allow for blocking?

}

trait FlowPool[T] extends FlowPoolLike[T, Future]

object FlowPool {

  trait Builder[T] {
    def <<(x: T): this.type
    def seal(): Unit
  }

}
