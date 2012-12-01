package scala.dataflow.array

import org.scalatest.FunSuite

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class HierFlowArraySuite extends FunSuite with FATestHelper {

  test("map on HierFA") {
    val n = 100
    val fa = nFA(n).flatMapN(n)(x => nFA(n)).map(_ * 2)
    verEls(fa)((x,i) => x == (i % n) * 2)
  }

  test("fold on HierFA") {
    val n = 500
    val bfa = nFA(n).flatMapN(n)(x => nFA(n))
    val fld = bfa.fold(0)(_ + _)
    assert(fld.blocking == n * (n-1) * n / 2)
  }

  test("zipMap on HierFA and FlatFA") {
    val isize = 1000
    val fa1 = nFA(size / isize).flatMapN(isize)(x => nFA(isize))
    val fa2 = nFA
    val res = (fa1 zipMap fa2)(_ + _)
    verEls(res)((x,i) => x == i + (i % isize))
  }

}
