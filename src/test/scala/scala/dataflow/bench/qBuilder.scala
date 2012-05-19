package scala.dataflow.bench

import java.util.Queue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedTransferQueue
import java.util.concurrent.ConcurrentLinkedQueue

trait QBuilder  { def newQ[T]: Queue[T] }
trait BQBuilder extends QBuilder { def newQ[T]: BlockingQueue[T] }

trait LTQBuilder extends BQBuilder {
  def newQ[T] = new LinkedTransferQueue[T]()
}

trait CLQBuilder extends QBuilder {
  def newQ[T] = new ConcurrentLinkedQueue[T]()
}
