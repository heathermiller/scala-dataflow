package scala.dataflow



import sun.misc.Unsafe



package object pool {

  trait Factory[+CC[X] <: FlowPool[X]] extends FlowFactory[CC] {

    def unfold[T](start: T*)(f: T => FlowPool[T]): CC[T] = {
      val fp = apply[T]()

      fp ++= (start: _*)

      val diverger = fp.diverger
      for (x <- diverger) {
        val more = f(x)
        diverger.depend(more)
        for (y <- more) fp += y
      }

      fp
    }

  }

}
