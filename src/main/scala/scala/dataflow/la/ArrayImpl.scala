package scala.dataflow.la

trait ArrayImpl {

  type Array[A]
  type CanFlat[A,B]
  type FoldResult[A]
  
  def tabulate[A : ClassManifest](n: Int)(f: Int => A): Array[A]

  implicit def array2View[A : ClassManifest](v: Array[A]): AbstractArray[A]
  implicit def foldResult2View[A : ClassManifest](v: FoldResult[A]): AbstractFoldResult[A]

  implicit def arManifest[A : ClassManifest]: ClassManifest[Array[A]]
  implicit def frManifest[A : ClassManifest]: ClassManifest[FoldResult[A]]

  implicit def flatFutInA[A : ClassManifest]: CanFlat[FoldResult[A], A]
  implicit def flatAInA[A : ClassManifest]: CanFlat[Array[A], A]

  trait AbstractArray[A] {
    def size: Int
    def map[B : ClassManifest](f: A => B): Array[B]
    def flatMapN[B : ClassManifest](n: Int)(f: A => Array[B]): Array[B]
    def zipMap[B : ClassManifest, C : ClassManifest](that: Array[B])(f: (A,B) => C): Array[C]
    def flatten[B](n: Int)(implicit flat: CanFlat[A,B], mf: ClassManifest[B]): Array[B]
    def partition(n: Int): Array[Array[A]]
    def fold[A1 >: A](z: A1)(op: (A1, A1) => A1): FoldResult[A1]
    def transpose(step: Int): Array[A]
    def blocking: scala.Array[A]
  }

  trait AbstractFoldResult[A] {
    def blocking: A
  }

}
