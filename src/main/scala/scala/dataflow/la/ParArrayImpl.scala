package scala.dataflow.la

import scala.collection.parallel.mutable.ParArray
import scala.collection.GenTraversableOnce

import scala.reflect.ClassTag
import scala.reflect.classTag

trait ParArrayImpl extends ArrayImpl {

  type CanFlat[A,B] = (A) => GenTraversableOnce[B]
  type Array[A] = ParArray[A]
  type FoldResult[A] = A

  implicit def array2View[A : ClassTag](pa: ParArray[A]) = new BoxedArray(pa)
  class BoxedArray[A : ClassTag](pa: ParArray[A]) extends AbstractArray[A] {
    def size = pa.size
    def map[B : ClassTag](f: A => B) = pa.map(f)
    def flatMapN[B : ClassTag](n: Int)(f: A => Array[B]) = pa.flatMap(f)
    def zipMap[B : ClassTag, C : ClassTag](that: Array[B])(f: (A,B) => C) =
      pa.zip(that).map(f.tupled)
    def zipMapFold[B : ClassTag, C](that: Array[B])(f: (A,B) => C)(z: C)(op: (C,C) => C) =
      pa.zip(that).map(f.tupled).fold(z)(op)
    def flatten[B](n: Int)(implicit flat: CanFlat[A,B], mf: ClassTag[B]) = pa.flatten
    def partition(n: Int) =
      ParArray.tabulate(n)(x => pa.slice(x * size / n, (x + 1) * size / n))
    def fold[A1 >: A](z: A1)(op: (A1, A1) => A1): FoldResult[A1] = pa.fold(z)(op)
    def transpose(step: Int) = partition(size / step).transpose(flatAInA).flatten(flatAInA)
    def blocking: scala.Array[A] = pa.toArray
  }

  implicit def foldResult2View[A : ClassTag](fr: FoldResult[A]) = new BoxedFoldResult(fr)
  class BoxedFoldResult[A : ClassTag](fr: FoldResult[A]) extends AbstractFoldResult[A] {
    def blocking = fr
  }

  implicit def frManifest[A : ClassTag] = classTag[A]
  // These are super ugly hacks... But they seem to work.
  implicit def arManifest[A : ClassTag] =
    classTag[Array[_]].asInstanceOf[ClassTag[Array[A]]]

  implicit def flatFutInA[A : ClassTag] = (x: A) => List(x)
  implicit def flatAInA[A : ClassTag] = (x: Array[A]) => x.seq

  def tabulate[A : ClassTag](n: Int)(f: Int => A) = ParArray.tabulate(n)(f)

}
