package scala.dataflow.array

import org.scalatest.FunSuite

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class FlowArraySuite extends FunSuite {

  val size = 10000

  def nFA: FlowArray[Int] = nFA(size)
  def nFA(s: Int): FlowArray[Int] = FlowArray.tabulate(s)(x => x)

  def verEls[A : ClassManifest](fa: FlowArray[A])(ver: (A, Int) => Boolean) = {
    assert(fa.blocking.zipWithIndex.forall(ver.tupled))
  }

  test("tabulate a FA") {
    val fa = nFA
    verEls(fa)(_ == _)
  }

  test("map a FlatFA once") {
    val fa = nFA
    val mfa = fa.map(_ * 2)
    verEls(mfa)(_ == 2*_)
  }

  test("map a FlatFA twice") {
    val fa = nFA
    val m1fa = fa.map(_ * 2)
    val m2fa = m1fa.map(_ * 2)
    verEls(m2fa)(_ == 4*_)
  }

  test("branch on FlatFA flow") {
    val fa = nFA
    val m1fa = fa.map(_ * 4)
    val m2fa = fa.map(_ * 3)
    verEls(m1fa)(_ == _ * 4)
    verEls(m2fa)(_ == _ * 3)
  }

  test("flatMap on FA") {
    val n = 500
    val fa = nFA(n)
    val fmfa = fa.flatMapN(n)(x => FlowArray.tabulate(n)(_ * x))
    verEls(fmfa) { (x,i) =>
      (i % n) * (i / n) == x
    }
  }

  test("map on HierFA") {
    val n = 100
    val fa = nFA(n).flatMapN(n)(x => nFA(n)).map(_ * 2)
    val b = fa.blocking
    //b.take(200).foreach(println _)
    verEls(fa)((x,i) => x == (i % n) * 2)
    //fa.blocking.take(200).foreach(println _)
  }

  test("fold on FlatFA") {
    val fa = nFA
    val fld = fa.fold(0)(_ + _)
    assert(fld.blocking == (size-1)*size / 2)
  }

  /*
  test("fold on HierFA") {
    val n = 500
    val bfa = nFA(n).flatMapN(n)(x => nFA(n))
    val fld = bfa.fold(0)(_ + _)
    assert(fld.blocking == n * (n-1) * n / 2)
  }
  */

}
