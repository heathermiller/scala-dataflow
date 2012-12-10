package scala.dataflow.array

import org.scalatest.FunSuite

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class FlatFlowArraySuite extends FunSuite with FATestHelper {

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

  test("flatten on FlatFA of FlatFA") {
    val n = 500
    val fa = nFA(n)
    val mfa = fa.map(x => FlowArray.tabulate(n)(_ * x))
    val ffa = mfa.flatten(n)(flattenFaInFa[Int], manifest[Int])
    verEls(ffa) { (x,i) =>
      (i % n) * (i / n) == x
    }
  }

  test("flatten on FlatFA of FoldFuture") {
    val n = 500
    val fa = nFA(n)
    val mfa = fa.map(x => FlowArray.tabulate(n)(_ * x))
    val fofa = mfa.map(_.fold(0)(_ + _))
    val ffa = fofa.flatten(1)(flattenFutInFa[Int], manifest[Int])

    verEls(ffa) { (x,i) =>
      (n + 1) * n / 2 * (i / n) == x
    }
  }

  test("fold on FlatFA") {
    val fa = nFA
    val fld = fa.fold(0)(_ + _)
    assert(block(fld) == (size-1)*size / 2)
  }

  test("fold on FlatFA preserves order") {
    val chars = 'a' to 'z'
    val fa = FlowArray(chars.map(_.toString) :_*)
    val fld = fa.fold("")(_ + _)
    assert(block(fld) == chars.mkString)
  }

  test("zipMap on two FlatFA") {
    val fa1 = nFA
    val fa2 = nFA
    val res = (fa1 zipMap fa2)(_ + _)
    verEls(res)((x,i) => x == 2*i)
  }

  test("zipMap on FlatFA and HierFA") {
    val isize = 1000
    val fa1 = nFA
    val fa2 = nFA(size / isize).flatMapN(isize)(x => nFA(isize))
    val res = (fa1 zipMap fa2)(_ + _)
    verEls(res)((x,i) => x == i + (i % isize))
  }

  test("zipMap on FlatFA and HierFA with small inner size") {
    val isize = 100
    val fa1 = nFA
    val fa2 = nFA(size / isize).flatMapN(isize)(x => nFA(isize))
    val res = (fa1 zipMap fa2)(_ + _)
    verEls(res)((x,i) => x == i + (i % isize))
  }

}
