package scala.dataflow.array

import org.scalatest.FunSuite

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class FlowArraySliceViewSuite extends FunSuite with FATestHelper {

  val slStart = size / 9
  val slEnd   = 8 * size / 9
  val slSize  = slEnd - slStart + 1

  def nSL = nFA.slice(slStart, slEnd)

  test("block on slice") {
    val sl = nSL
    verEls(sl)(_ == _ + slStart)
  }

  test("map on slice") {
    val fa1 = nSL
    val fa2 = fa1.map(_ * 2)
    verEls(fa2)((x,i) => x == 2 * (i + slStart))
  }

  test("fold on slice") {
    val fa1 = nSL
    val res = fa1.fold(0)(_ + _)
    val v = res.blocking
    assert(v == (slEnd + 1) * slEnd / 2 - (slStart - 1) * slStart / 2)
  }

  test("zipMap on FASliceView and FlatFA") {
    val sl = nSL
    val fl = nFA(slSize)
    val res = (sl zipMap fl)(_ + _)
    verEls(res)(_ == 2 * _ + slStart)
  }

  test("map of a slice of a slice") {
    val is = 10
    val ie = 1000

    val isl = nSL.slice(is, ie)
    val res = isl.map(_ + 1)
    verEls(res)(_ == _ + 1 - slStart - is)
  }

}
