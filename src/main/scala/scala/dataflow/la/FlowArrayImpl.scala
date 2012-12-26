package scala.dataflow.la

import scala.dataflow.array._
import scala.reflect.ClassTag
import scala.reflect.classTag

trait FlowArrayImpl extends ArrayImpl {

  type CanFlat[A,B] = CanFlatten[A,B]
  type Array[A] = FlowArray[A]
  type FoldResult[A] = FoldFuture[A]

  implicit def array2View[A : ClassTag](fa: FlowArray[A]) = new BoxedArray(fa)
  class BoxedArray[A : ClassTag](fa: FlowArray[A]) extends AbstractArray[A] {
    def size = fa.size
    def map[B : ClassTag](f: A => B) = fa.map(f)
    def flatMapN[B : ClassTag](n: Int)(f: A => Array[B]) = fa.flatMapN(n)(f)
    def zipMap[B : ClassTag, C : ClassTag](that: Array[B])(f: (A,B) => C) =
      (this.fa zipMap that)(f)
    def zipMapFold[B : ClassTag, C](that: Array[B])(f: (A,B) => C)(z: C)(op: (C,C) => C) =
      fa.zipMapFold(that)(f)(z)(op)
    def flatten[B](n: Int)(implicit flat: CanFlat[A,B], mf: ClassTag[B]) =
      this.fa.flatten(n)
    def partition(n: Int) = fa.partition(n)
    def fold[A1 >: A](z: A1)(op: (A1, A1) => A1): FoldResult[A1] = fa.fold(z)(op)
    def transpose(step: Int) = fa.transpose(step)
    def blocking: scala.Array[A] = fa.blocking
  }

  implicit def foldResult2View[A : ClassTag](fr: FoldResult[A]) = new BoxedFoldResult(fr)
  class BoxedFoldResult[A : ClassTag](fr: FoldResult[A]) extends AbstractFoldResult[A] {
    def blocking = fr.blocking
  }

  // These are super ugly hacks... But they seem to work.
  implicit def frManifest[A : ClassTag] =
    classTag[FoldResult[_]].asInstanceOf[ClassTag[FoldResult[A]]]
  implicit def arManifest[A : ClassTag] =
    classTag[Array[_]].asInstanceOf[ClassTag[Array[A]]]

  implicit def flatFutInA[A : ClassTag] = flattenFutInFa
  implicit def flatAInA[A : ClassTag]   = flattenFaInFa

  def tabulate[A : ClassTag](n: Int)(f: Int => A) = FlowArray.tabulate(n)(f)

}
