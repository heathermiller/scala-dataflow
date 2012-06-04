package scala.dataflow

class SumFuture[T](vals: Int)(implicit num: Numeric[T]) extends Future[T] {
  
  var cnt: Int = 0
  var acc: T = num.zero

  override private[dataflow] def tryComplete(v: T): Boolean = {
    synchronized {
      if (cnt >= vals) return false
      acc = num.plus(acc, v)
      cnt = cnt + 1
      
      if (cnt >= vals)
        return super.tryComplete(acc)
      else
        true
    }
  }

}
