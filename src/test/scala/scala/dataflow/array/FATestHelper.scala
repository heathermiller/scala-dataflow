package scala.dataflow.array

trait FATestHelper {

  val size = 10000
  val timeout = 1000L // 1s

  //def faBlock[A](fa: FlowArray[A]) = fa.blocking
  def faBlock[A](fa: FlowArray[A]) = fa.blocking(false, timeout)

  def nFA: FlowArray[Int] = nFA(size)
  def nFA(s: Int): FlowArray[Int] = FlowArray.tabulate(s)(x => x)

  def verEls[A : ClassManifest](fa: FlowArray[A])(ver: (A, Int) => Boolean) = {
    assert(faBlock(fa).zipWithIndex.forall(ver.tupled))
  }

}
