package scala.dataflow

trait Builder[T] {
  def <<(x: T): this.type
  def seal(size: Int): Unit
}
