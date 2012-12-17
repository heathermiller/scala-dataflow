package scala.dataflow.la

import org.scalatest.FunSuite

abstract class LASuite extends FunSuite with ArrayLA {
  this: ArrayImpl =>

  val n = 200

  def verEls(d: Data)(ver: (Double, Int) => Boolean): Unit
  def block(d: Scalar): Double

  test("create a matrix") {
    val m = ones(n, n)
    verEls(m.data)((x,i) => x == 1)
  }

  test("create a vector") {
    val v = ones(n)
    verEls(v.data)((x,i) => x == 1)
  }

  test("scalar product") {
    val v1 = ones(n)
    val v2 = ones(n)
    assert(block(v1 * v2) == n)
  }

  test("matrix vector product") {
    val m = ones(n,n)
    val v = ones(n)
    val res = m * v
    verEls(res.data)((x,i) => x == n)
  }

  test("matrix matrix product") {
    val M = ones(n, 2*n)
    val N = ones(2*n, 3*n)
    val res = M * N
    verEls(res.data)((x,i) => x == 2 * n)
  }

  test("transpose a square matrix") {
    val M = ones(n, n)
    val res = M.t
    verEls(res.data)((x,i) => x == 1)
  }

  test("transpose a rectangular matrix") {
    val M = ones(2 * n, 3 * n)
    val res = M.t
    verEls(res.data)((x,i) => x == 1)
  }

}
