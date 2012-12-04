package scala.dataflow

import sun.misc.Unsafe

package object array {
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

  trait CanFlatten[A,B] {
    def flatten(fa: FlatFlowArray[A], n: Int): FlowArray[B]
  }

  implicit def flattenFutInFa[A : ClassManifest] = new CanFlatten[Future[A], A] {
    def flatten(fa: FlatFlowArray[Future[A]], n: Int) = {
      /*
      val res = new HierFlowArray(fa.data, n)
      res.generatedBy(fa)
      res
      */
      // TODO
      null
    }
  }

  implicit def flattenFaInFa[A : ClassManifest] = new CanFlatten[FlowArray[A], A] {
    def flatten(fa: FlatFlowArray[FlowArray[A]], n: Int) = {
      val res = new HierFlowArray(fa.data, n)
      res.generatedBy(fa)
      res
    }
  }



}
