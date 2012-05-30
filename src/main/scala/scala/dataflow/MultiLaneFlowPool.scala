package scala.dataflow

import scala.annotation.tailrec
import jsr166y._

class MultiLaneFlowPool[T](val lanes: Int) extends FlowPool[T] {

  import FlowPool._
  import MultiLaneFlowPool._
  
  val initBlocks = Array.fill(lanes)(newBlock(0))
  val sealHolder = new MLSealHolder()
  
  def newPool[S] = new MultiLaneFlowPool[S](lanes)

  def builder = new MultiLaneBuilder[T](initBlocks, sealHolder)

  def aggregate[S](zero: S)(cmb: (S, S) => S)(folder: (S, T) => S): Future[S] = {
    val a = FlowAggregate[S](zero)(cmb)
    a.seal(lanes)
    
    for (b <- initBlocks) {
      val cbe = new CallbackElem[T, S](folder, (sz, acc) => a.aggregate(acc), CallbackNil, b, 0, zero)
      task(new RegisterCallbackTask(cbe))
    }
    
    a.future
  }
  
  // def doForAll[U](f: T => U): Future[Int] = {
  //   val fut = new SumFuture[Int](lanes)

  //   /* Note: Final sync of callback goes through SumFuture */
  //   for (b <- initBlocks) {
  //     val cbe = new CallbackElem(f, fut.complete _, CallbackNil, b, 0)
  //     task(new RegisterCallbackTask(cbe))
  //   }

  //   fut
  // }

  // override def mapFold[U, V >: U](accInit: V)(cmb: (V,V) => V)(map: T => U): Future[V] = {

  //   val fut = new SumFuture[Int](lanes)

  //   def accf(h: AccHolder[V])(x: T) =
  //     h.acc = cmb(map(x), h.acc)

  //   val holders = initBlocks map { b =>
  //     val h = new AccHolder(accInit)
  //     val cbe = new CallbackElem[T, Unit]((acc, x) => accf(h)(x), (sz, acc) => fut.complete(sz), CallbackNil, b, 0, ())
  //     task(new RegisterCallbackTask(cbe))
  //     h                            
  //   }

  //   fut map {
  //     c => holders.map(_.acc).reduce(cmb)
  //   }

  // }

}

object MultiLaneFlowPool {

  final class AccHolder[T](init: T) {
    /* We do not need to synchronize on this var, because IN THE
     * CURRENT SETTING, callbacks are only executed in sequence ON EACH BLOCK
     * This WILL break if the scheduling changes
     */
    @volatile var acc = init
  }

}
