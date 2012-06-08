package scala.dataflow
package impl



import scala.annotation.tailrec
import jsr166y._



class SingleLaneFlowPool[T] extends FlowPool[T] {

  import FlowPool._
  
  val initBlock = newBlock(0)
  
  def newPool[S] = new SingleLaneFlowPool[S]()

  def builder = new SingleLaneBuilder[T](initBlock)
  
  def aggregate[S](zero: =>S)(cmb: (S, S) => S)(folder: (S, T) => S): Future[S] = {
    val fut = new Future[S]()
    val cbe = new CallbackElem[T, S](folder, (sz, acc) => fut complete acc, CallbackNil, initBlock, 0, zero)
    task(new RegisterCallbackTask(cbe))
    fut
  }
  
}


object SingleLangeFlowPool {
  
}
