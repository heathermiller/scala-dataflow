package scala.dataflow.la

import org.scalatest.FunSuite

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ParArrayImplSuite extends FunSuite with ParArrayImpl {

  val n = 1000
  def nPA = tabulate(n)(x => x)(manifest[Int])

  test("partition returns correct size sub arrays") {
    val parts = 10
    val p = array2View(nPA)(manifest[Int]).partition(parts)
    p.foreach { x =>
      assert(x.size == n / parts)
    }
  }

  test("transpose on array") {
    val step = 100
    val res = array2View(nPA)(manifest[Int]).transpose(step)
  }

}
