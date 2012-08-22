package scala.dataflow



trait Builder[T] {
  def +=(x: T): this.type
  def seal(size: Int): Unit

  def ++=(xs: T*): this.type = {
  	for (x <- xs) this += x
  	this
  }
}
