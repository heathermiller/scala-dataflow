package scala.dataflow.la

import scala.reflect.ClassTag

trait ArrayImpl {

  type Array[A]
  type CanFlat[A,B]
  type FoldResult[A]
  
  def tabulate[A : ClassTag](n: Int)(f: Int => A): Array[A]

  implicit def array2View[A : ClassTag](v: Array[A]): AbstractArray[A]
  implicit def foldResult2View[A : ClassTag](v: FoldResult[A]): AbstractFoldResult[A]

  implicit def arManifest[A : ClassTag]: ClassTag[Array[A]]
  implicit def frManifest[A : ClassTag]: ClassTag[FoldResult[A]]

  implicit def flatFutInA[A : ClassTag]: CanFlat[FoldResult[A], A]
  implicit def flatAInA[A : ClassTag]: CanFlat[Array[A], A]

  trait AbstractArray[A] {
    def size: Int
    def map[B : ClassTag](f: A => B): Array[B]
    def flatMapN[B : ClassTag](n: Int)(f: A => Array[B]): Array[B]
    def zipMap[B : ClassTag, C : ClassTag](that: Array[B])(f: (A,B) => C): Array[C]
    def zipMapFold[B : ClassTag, C](that: Array[B])(f: (A,B) => C)
                                        (z: C)(op: (C,C) => C): FoldResult[C]
    def flatten[B](n: Int)(implicit flat: CanFlat[A,B], mf: ClassTag[B]): Array[B]
    def partition(n: Int): Array[Array[A]]
    def fold[A1 >: A](z: A1)(op: (A1, A1) => A1): FoldResult[A1]
    def transpose(step: Int): Array[A]
    def blocking: scala.Array[A]
  }

  trait AbstractFoldResult[A] {
    def blocking: A
  }

}
