package scala.dataflow

import scala.annotation.tailrec
import jsr166y._

class MultiLaneFlowPool[T](val lanes: Int) extends FlowPool[T] {

  import FlowPool._
  
  val initBlocks = Array.fill(lanes)(newBlock(0))
  val sealHolder = new MLSealHolder()
  
  def newPool[S] = new MultiLaneFlowPool[S](lanes)

  def builder = new MultiLaneBuilder[T](initBlocks, sealHolder)

  def doForAll[U](f: T => U): Future[Int] = {
    val fut = new Future[Int]()

    // TODO find way to sync CB completion
    // Have a thing that, when called N times, fetches Seal count and ends
    for (b <- initBlocks) {
      val cbe = new CallbackElem(f, fut.complete _, CallbackNil, initBlocks(0), 0)
      task(new RegisterCallbackTask(cbe))
    }

    fut
  }

  // TODO the following is uttermost bullshit when having multiple lanes!!!
  def mappedFold[U, V <: U](accInit: V)(cmb: (U,V) => V)(map: T => U): Future[(Int, V)] = {
    /* We do not need to synchronize on this var, because IN THE
     * CURRENT SETTING, callbacks are only executed in sequence.
     * This WILL break if the scheduling changes
     */
    @volatile var acc = accInit

    doForAll { x =>
      acc = cmb(map(x), acc)
    } map {
      c => (c,acc)
    }
  }

}
