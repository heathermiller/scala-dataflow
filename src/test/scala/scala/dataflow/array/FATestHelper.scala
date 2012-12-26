package scala.dataflow.array

import scala.dataflow.Future
import scala.reflect.ClassTag

trait FATestHelper {

  val size = 10000
  val timeout = 1000L // 1s

  //def faBlock[A](fa: FlowArray[A]) = fa.blocking
  def block[A](fa: FlowArray[A]) = fa.blocking(false, timeout)
  def block[A](ft: Future[A])    = ft.blocking(false, timeout)

  def nFA: FlowArray[Int] = nFA(size)
  def nFA(s: Int): FlowArray[Int] = FlowArray.tabulate(s)(x => x)

  def verEls[A : ClassTag](fa: FlowArray[A])(ver: (A, Int) => Boolean) = {
    assert(block(fa).zipWithIndex.forall(ver.tupled))
  }

}
