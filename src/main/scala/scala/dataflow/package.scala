package scala



import sun.misc.Unsafe



package object dataflow {
  
  def future[T](x: T) = {
    val f = new Future[T]()
    f.complete(x)
    f
  }

  def futLift[A,B,C](f: (A,B) => C) = {
    (f1: Future[A], f2: Future[B]) =>
      f1.flatMap(x => f2.map(y => f(x,y)))
  }
  
  def getUnsafe(): Unsafe = {
    // Not on bootclasspath
    if (this.getClass.getClassLoader == null) Unsafe.getUnsafe()
    try {
      val fld = classOf[Unsafe].getDeclaredField("theUnsafe")
      fld.setAccessible(true)
      return fld.get(this.getClass).asInstanceOf[Unsafe]
    } catch {
      case e => throw new RuntimeException("Could not obtain access to sun.misc.Unsafe", e)
    }
  }

}


package dataflow {
  
}
