package scala.dataflow
package pool






final class Diverger[T](fp: FlowPool[T]) {

  def foreach[U](f: T => U): Future[Int] = {
    null
  }

  def depend(fp: FlowPool[T]) {
    // TODO
  }

}
