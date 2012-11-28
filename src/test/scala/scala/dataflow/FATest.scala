package scala.dataflow

import array._

object FATest extends App {

  /*
  val n = 10000

  val fa1 = FlowArray.tabulate(n)(x => x*x toLong)
  /*
  val fa2 = fa1.flatMapN(n)(mkFA _)
  val fa3 = fa2.map(_ / 2)
  val fut = fa3.fold(0L)(_ + _)
  */
  val fut = fa1.fold(0L)(_ + _)

  println(fut.blocking)

  def mkFA(v: Long) = {
    FlowArray.tabulate(n)(x => x*v)
  }
  */

  /*
  val fa = (FlowArray.tabulate(100)(x => x * 100) flatMapN 100) { x =>
    FlowArray.tabulate(100)(y => x + y)
  } map { x => if (x % 100 == 0) {println("mapping: " + x )}; x }

  val res = fa.blocking

  println("done")
  */

  val size = 10000
  val timeout = 10000L // 10s

  def faBlock[A](fa: FlowArray[A]) = fa.blocking
  //def faBlock[A](fa: FlowArray[A]) = fa.blocking(false, timeout)

  def nFA: FlowArray[Int] = nFA(size)
  def nFA(s: Int): FlowArray[Int] = FlowArray.tabulate(s)(x => x)

  def verEls[A : ClassManifest](fa: FlowArray[A])(ver: (A, Int) => Boolean) = {
    assert(faBlock(fa).zipWithIndex.forall(ver.tupled))
  }

  val isize = 100
  val fa1 = nFA
  val fa2 = nFA(size / isize).flatMapN(isize)(x => nFA(isize))
  val res = (fa1 zipMap fa2)(_ + _)

  val vals = res.blocking

  for ((x,i) <- vals.zipWithIndex if x != i + (i % isize)) {
    println("wrong at %d: should: %d, is: %d".format(i, i + (i % isize), x))
  }

}
