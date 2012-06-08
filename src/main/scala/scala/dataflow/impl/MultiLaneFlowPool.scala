package scala.dataflow
package impl

import scala.annotation.tailrec
import jsr166y._

class MultiLaneFlowPool[T](val lanes: Int) extends FlowPool[T] {

  import FlowPool._
  import MultiLaneFlowPool._
  
  val initBlocks = Array.fill(lanes)(newBlock(0))
  val sealHolder = new MLSealHolder()
  
  def newPool[S] = new MultiLaneFlowPool[S](lanes)

  def builder = new MultiLaneBuilder[T](initBlocks, sealHolder)

  def aggregate[S](zero: =>S)(cmb: (S, S) => S)(folder: (S, T) => S): Future[S] = {
    val a = FlowLatch[S](zero)(cmb)
    a.seal(lanes)
    
    for (b <- initBlocks) {
      val cbe = new CallbackElem[T, S](folder, (sz, acc) => a << acc, CallbackNil, b, 0, zero)
      task(new RegisterCallbackTask(cbe))
    }
    
    a.future
  }
  
}

object MultiLaneFlowPool {
  
}
