package scala



import sun.misc.Unsafe



package object dataflow {
  
  def future[T](x: T) = {
    val f = new Future[T]()
    f.complete(x)
    f
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
