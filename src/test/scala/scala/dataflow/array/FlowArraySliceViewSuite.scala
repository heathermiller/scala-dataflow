package scala.dataflow.array

import org.scalatest.FunSuite

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class FlowArraySliceViewSuite extends FunSuite with FATestHelper {

  val slStart = size / 9
  val slEnd   = 8 * size / 9

  def nSL = nFA.slice(slStart, slEnd)

  test("block on slice") {
    val sl = nSL
    verEls(sl)((x,i) => x == i + slStart)
  }

  test("map on slice") {
    val fa1 = nSL
    val fa2 = fa1.map(_ * 2)
    verEls(fa2)((x,i) => x == (i + slStart) * 2)
  }

}
