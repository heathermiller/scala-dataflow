package scala.dataflow.la

import scala.collection.parallel.mutable.ParArray

trait ParArrayImpl extends ArrayImpl {

  type CanFlat[A,B] = (A) => TraversableOnce[B]
  type Array[A] = ParArray[A]
  type FoldResult[A] = A

  implicit def array2View[A : ClassManifest](pa: ParArray[A]) = new BoxedArray(pa)
  class BoxedArray[A : ClassManifest](pa: ParArray[A]) extends AbstractArray[A] {
    def size = pa.size
    def map[B : ClassManifest](f: A => B) = pa.map(f)
    def flatMapN[B : ClassManifest](n: Int)(f: A => Array[B]) = pa.flatMap(f)
    def zipMap[B : ClassManifest, C : ClassManifest](that: Array[B])(f: (A,B) => C) =
      pa.zip(that).map(f.tupled)
    def zipMapFold[B : ClassManifest, C](that: Array[B])(f: (A,B) => C)(z: C)(op: (C,C) => C) =
      pa.zip(that).map(f.tupled).fold(z)(op)
    def flatten[B](n: Int)(implicit flat: CanFlat[A,B], mf: ClassManifest[B]) = pa.flatten
    def partition(n: Int) =
      ParArray.tabulate(n)(x => pa.slice(x * size / n, (x + 1) * size / n))
    def fold[A1 >: A](z: A1)(op: (A1, A1) => A1): FoldResult[A1] = pa.fold(z)(op)
    def transpose(step: Int) = partition(size / step).transpose(flatAInA).flatten
    def blocking: scala.Array[A] = pa.toArray
  }

  implicit def foldResult2View[A : ClassManifest](fr: FoldResult[A]) = new BoxedFoldResult(fr)
  class BoxedFoldResult[A : ClassManifest](fr: FoldResult[A]) extends AbstractFoldResult[A] {
    def blocking = fr
  }

  implicit def frManifest[A : ClassManifest] = classManifest[A]
  // These are super ugly hacks... But they seem to work.
  implicit def arManifest[A : ClassManifest] =
    classManifest[Array[_]].asInstanceOf[ClassManifest[Array[A]]]

  implicit def flatFutInA[A : ClassManifest] = (x: A) => List(x)
  implicit def flatAInA[A : ClassManifest] = (x: Array[A]) => x.seq

  def tabulate[A : ClassManifest](n: Int)(f: Int => A) = ParArray.tabulate(n)(f)

}
