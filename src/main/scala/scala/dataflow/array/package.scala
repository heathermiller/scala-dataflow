package scala.dataflow

import sun.misc.Unsafe
import scala.reflect.ClassTag

package object array {

  /** retrieve a sun.misc.Unsafe */
  def getUnsafe(): Unsafe = {
    // Not on bootclasspath
    if (this.getClass.getClassLoader == null) Unsafe.getUnsafe()
    try {
      val fld = classOf[Unsafe].getDeclaredField("theUnsafe")
      fld.setAccessible(true)
      return fld.get(this.getClass).asInstanceOf[Unsafe]
    } catch {
      case e: Exception =>
        throw new RuntimeException("Could not obtain access to " +
                                   "sun.misc.Unsafe", e)
    }
  }

  /**
   * trait for implicit evidence about flattening ability of a
   * FlowArray
   */
  trait CanFlatten[A,B] {
    def flatten(fa: FlatFlowArray[A], n: Int): FlowArray[B] 
  }

  /**
   * How to flatten a FlatFlowArray[FoldFuture[A]]
   */
  implicit def flattenFutInFa[A : ClassTag] = 
    new CanFlatten[FoldFuture[A], A] {
      def flatten(fa: FlatFlowArray[FoldFuture[A]], n: Int) = {
        require(n == 1)

        // Consolidate futures (wait for completion in chunks)
        val cjob = FAFoldConsolidateJob(fa, 0, fa.size)
        fa.dispatch(cjob, 0, fa.size)
        
        // Flatten result
        val res = new FlatFlowArray(new Array[A](fa.size))
        val g = FAMapJob(res, (x: FoldFuture[A]) => x.get)
        val mjob = g(fa ,0, 0, fa.size)
        res.generatedBy(mjob)
        cjob.depending(mjob)
        
        res
      }
    }

  /**
   * How to flatten a FlatFlowArray[FlowArray[A]]
   */
  implicit def flattenFaInFa[A : ClassTag] = new CanFlatten[FlowArray[A], A] {
    def flatten(fa: FlatFlowArray[FlowArray[A]], n: Int) = {
      val res = new HierFlowArray(fa.data, n)
      res.generatedBy(fa)
      res
    }
  }



}
