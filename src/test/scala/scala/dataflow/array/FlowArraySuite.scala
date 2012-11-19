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

  test("map a FA once") {
    val fa = nFA
    val mfa = fa.map(_ * 2)
    verEls(mfa)(_ == 2*_)
  }

  test("map a FA twice") {
    val fa = nFA
    val m1fa = fa.map(_ * 2)
    val m2fa = m1fa.map(_ * 2)
    verEls(m2fa)(_ == 4*_)
  }

  test("branch on FA flow") {
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

  

}
