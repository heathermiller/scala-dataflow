package scala



import sun.misc.Unsafe
import language.experimental.macros
import scala.reflect.makro.Context



package object dataflow {
  
  def future[T](x: T) = {
    val f = new Future[T]()
    f.complete(x)
    f
  }
  
  private[dataflow] def getUnsafe(): Unsafe = {
    if (this.getClass.getClassLoader == null) Unsafe.getUnsafe()
    try {
      val fld = classOf[Unsafe].getDeclaredField("theUnsafe")
      fld.setAccessible(true)
      return fld.get(this.getClass).asInstanceOf[Unsafe]
    } catch {
      case e => throw new RuntimeException("Could not obtain access to sun.misc.Unsafe", e)
    }
  }

  /* macros */

  def seal[T](body: T): T = macro seal_impl[T]

  def seal_impl[T](c: Context)(body: c.Expr[T]): c.Expr[T] = {
    import c.universe._

    // analyze body

    // invoke special seal at the end if that is safe

    body
  }

}


package dataflow {
  
  trait FlowFactory[+CC[_]] {

    def apply[T](): CC[T]

    def unfold[T](start: T*)(f: T => FlowPool[T]): CC[T]

  }

}
