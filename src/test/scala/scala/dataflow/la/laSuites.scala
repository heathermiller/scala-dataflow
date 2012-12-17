package scala.dataflow.la

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import scala.dataflow.array.FATestHelper

@RunWith(classOf[JUnitRunner])
class FALASuite extends LASuite with FATestHelper with FlowArrayImpl {
  def block(d: Scalar) = block[Double](d)
  def verEls(d: Data)(ver: (Double, Int) => Boolean) = verEls[Double](d)(ver)
  override val timeout = 10000L // 10s
}

@RunWith(classOf[JUnitRunner])
class PALASuite extends LASuite with FATestHelper with ParArrayImpl {
  def block(d: Scalar) = d
  def verEls(d: Data)(ver: (Double, Int) => Boolean) = 
    assert(d.zipWithIndex.forall(ver.tupled))
}
