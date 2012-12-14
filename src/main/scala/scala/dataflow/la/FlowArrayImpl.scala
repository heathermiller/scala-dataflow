package scala.dataflow.la

import scala.dataflow.array._

trait FlowArrayImpl extends ArrayImpl {

  type CanFlat[A,B] = CanFlatten[A,B]
  type Array[A] = FlowArray[A]
  type FoldResult[A] = FoldFuture[A]

  implicit def array2View[A : ClassManifest](fa: FlowArray[A]) = new BoxedArray(fa)
  class BoxedArray[A : ClassManifest](fa: FlowArray[A]) extends AbstractArray[A] {
    def size = fa.size
    def map[B : ClassManifest](f: A => B) = fa.map(f)
    def zipMap[B : ClassManifest, C : ClassManifest](that: Array[B])(f: (A,B) => C) =
      (this.fa zipMap that)(f)
    def flatten[B](n: Int)(implicit flat: CanFlat[A,B], mf: ClassManifest[B]) =
      this.fa.flatten(n)
    def partition(n: Int) = fa.partition(n)
    def fold[A1 >: A](z: A1)(op: (A1, A1) => A1): FoldResult[A1] = fa.fold(z)(op)
    def blocking: scala.Array[A] = fa.blocking
  }

  implicit def foldResult2View[A : ClassManifest](fr: FoldResult[A]) = new BoxedFoldResult(fr)
  class BoxedFoldResult[A : ClassManifest](fr: FoldResult[A]) extends AbstractFoldResult[A] {
    def blocking = fr.blocking
  }

  // These are super ugly hacks... But they seem to work.
  implicit def frManifest[A : ClassManifest] =
    classManifest[FoldResult[_]].asInstanceOf[ClassManifest[FoldResult[A]]]
  implicit def arManifest[A : ClassManifest] =
    classManifest[Array[_]].asInstanceOf[ClassManifest[Array[A]]]

  implicit def flatFutInA[A : ClassManifest] = flattenFutInFa
  implicit def flatAInA[A : ClassManifest]   = flattenFaInFa

  def tabulate[A : ClassManifest](n: Int)(f: Int => A) = FlowArray.tabulate(n)(f)

}
