package scala.dataflow

import scala.annotation.tailrec
import jsr166y._

sealed trait CallbackHolder[-T] {
  def callbacks: CallbackList[T]
  def insertedCallback[U <: T](el: CallbackElem[U]): CallbackHolder[U]
}


sealed class CallbackList[-T] extends CallbackHolder[T] {
  def callbacks = this
  def insertedCallback[U <: T](el: CallbackElem[U]): CallbackList[U] =
    new CallbackElem(
      el.func,
      el.endf,
      this,
      el.block,
      el.pos
    )
}

final object CallbackNil extends CallbackList[Any]

final case class Seal[T](size: Int, callbacks: CallbackList[T]) extends CallbackHolder[T] {
  def insertedCallback[U <: T](el: CallbackElem[U]) = Seal(size, callbacks.insertedCallback(el))
}

final case class SealTag[T](
    p: MLSealHolder.Proposition,
    callbacks: CallbackList[T]
) extends CallbackHolder[T] {
  def insertedCallback[U <: T](el: CallbackElem[U]) =
    SealTag(p, callbacks.insertedCallback(el))
  def toSeal(size: Int) = Seal(size, callbacks)
}

final case object MustExpand

final case class Next(val block: Array[AnyRef]) {
  @volatile var index: Int = 0
}

final class CallbackElem[-T] (
  val func: T => Any,
  val endf: Int => Any,
  val next: CallbackList[T],
  var block: Array[AnyRef],
  var pos: Int
) extends CallbackList[T] {
  @volatile var lock: Int = -1
  
  /* ATTENTION:
   * If you change the scheduling, make sure that SingleLaneFlowPool.mappedFold
   * synchronized still properly. Otherwise there will be races.
   */
  @tailrec
  def awakeCallback() {
    val lk = /*READ*/lock
    if (lk < 0) {
      // there is no active batch
      if (tryOwn()) {
        // we are now responsible for starting the active batch
        // so we start a new fork-join task to call the callbacks
        FlowPool.task(new CallbackElem.BatchTask(this))
      } else awakeCallback()
    }
  }
  
  def tryOwn(): Boolean = CallbackElem.CAS_COUNT(this, -1, 1)
  
  def unOwn() = CallbackElem.WRITE_COUNT(this, -1)
}

object CallbackElem {
  
  val unsafe = getUnsafe()
  
  val COUNTOFFSET = unsafe.objectFieldOffset(classOf[CallbackElem[_]].getDeclaredField("lock"))
  
  def CAS_COUNT(obj: CallbackElem[_], ov: Int, nv: Int) = unsafe.compareAndSwapInt(obj, COUNTOFFSET, ov, nv)
  
  def WRITE_COUNT(obj: CallbackElem[_], v: Int) = unsafe.putIntVolatile(obj, COUNTOFFSET, v)
  
  final class BatchTask[T](val callback: CallbackElem[T]) extends RecursiveAction {
    import FlowPool._
    
    // when entering this method, we have to hold the lock!
    @tailrec
    protected def compute() {
      if (callback.pos >= LAST_CALLBACK_POS) {
        callback.block(MUST_EXPAND_POS) match {
          case MustExpand => // don't do anything
          case Next(b) =>
            callback.block = b
            callback.pos = 0
        }
      }
      
      // invoke callback while there are elements or we reach an end of the block
      val block = callback.block
      var pos = callback.pos
      var cur = block(pos)
      while (!cur.isInstanceOf[CallbackHolder[_]]) {
        callback.func(cur.asInstanceOf[T])
        pos += 1
        cur = block(pos)
      }
      callback.pos = pos

      // Check for seal
      cur match {
        case Seal(sz, null) => callback.endf(sz)
        case _ =>
      }
      
      // relinquish control
      callback.unOwn()
      
      // see if there are more elements available
      // if there are, try to regain control and start again

      if (callback.pos >= LAST_CALLBACK_POS) {
        block(MUST_EXPAND_POS) match {
          case MustExpand => // done
          case Next(b) => if (callback.tryOwn()) {
            callback.block = b
            callback.pos = 0
            FlowPool.task(new CallbackElem.BatchTask(callback))
          }
        }
      } else {
        cur = block(pos)
        cur match {
          case Seal(sz, null) =>
            // Shortcut
            if (callback.tryOwn()) {
              callback.endf(sz)
              callback.unOwn()
            }
          case _: CallbackHolder[_] =>
          case _ => if (callback.tryOwn()) { compute() }
        }
      }
    }
  }
  
}
