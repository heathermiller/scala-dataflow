package scala.dataflow

trait FlowPool[T] {
  
  def builder: Builder[T]
  def doForAll[U](f: T => U): Future[Int]
  def mappedFold[U, V <: U](accInit: V)(cmb: (U,V) => V)(map: T => U): Future[(Int, V)]
  def newPool[S]: FlowPool[S]

  // Monadic Ops

  def exists(f: T => Boolean): Future[Boolean] = {
    val fut = new Future[Boolean]
    doForAll { x =>
      if (f(x)) fut.tryComplete(true)
    } map { c =>
      fut.tryComplete(false)
    }
    fut
  }

  def map[S](f: T => S): FlowPool[S] = {
    val fp = newPool[S]
    val b  = fp.builder

    doForAll { x =>
      b << f(x)
    } map { b.seal _ }

    fp
  }

  def foreach[U](f: T => U) { doForAll(f) }

  def filter(f: T => Boolean): FlowPool[T] = {
    val fp = newPool[T]
    val b  = fp.builder

    mappedFold(0)(_ + _) { x =>
      if (f(x)) { b << x; 1 }
      else 0
    }  map {
      case (c,fc) => b.seal(fc)
    }

    fp
  }

  def flatMap[S](f: T => FlowPool[S]): FlowPool[S] = {
    val fp = newPool[S]
    val b  = fp.builder

    mappedFold(future(0))(futLift(_ + _)) { x =>
      val ifp = f(x)
      ifp.doForAll(b << _)
    } map { 
      case (c,cfut) => cfut.map(b.seal _)
    }

    fp
  }

}
