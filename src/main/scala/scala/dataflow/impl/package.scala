package scala.dataflow



import sun.misc.Unsafe



package object impl {
  def getUnsafe(): Unsafe = {
    // Not on bootclasspath
    if (this.getClass.getClassLoader == null) Unsafe.getUnsafe()
    try {
      val fld = classOf[Unsafe].getDeclaredField("theUnsafe")
      fld.setAccessible(true)
      return fld.get(this.getClass).asInstanceOf[Unsafe]
    } catch {
      case e: Exception => throw new RuntimeException("Could not obtain access to sun.misc.Unsafe", e)
    }
  }
}
