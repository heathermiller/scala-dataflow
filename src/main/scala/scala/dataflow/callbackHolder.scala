package scala.dataflow

import scala.annotation.tailrec
import jsr166y._

sealed trait CallbackHolder[-T] {
  def callbacks: CallbackList[T]
  def insertedCallback[U <: T, S](el: CallbackElem[U, S]): CallbackHolder[U]
}


sealed class CallbackList[-T] extends CallbackHolder[T] {
  def callbacks = this
  def insertedCallback[U <: T, S](el: CallbackElem[U, S]): CallbackList[U] =
    new CallbackElem(
      el.folder,
      el.finalizer,
      this,
      el.block,
      el.position,
      el.accumulator
    )
}

final object CallbackNil extends CallbackList[Any]

final case class Seal[T](size: Int, callbacks: CallbackList[T]) extends CallbackHolder[T] {
  def insertedCallback[U <: T, S](el: CallbackElem[U, S]) = Seal(size, callbacks.insertedCallback(el))
}

final case class SealTag[T](
    p: MLSealHolder.Proposition,
    callbacks: CallbackList[T]
) extends CallbackHolder[T] {
  def insertedCallback[U <: T, S](el: CallbackElem[U, S]) =
    SealTag(p, callbacks.insertedCallback(el))
  def toSeal(cursz: Int, addsz: Int) = {
    val nsz = cursz + addsz
    if (addsz > 0) Seal(nsz, callbacks) else Seal(nsz, null)
  }
}

final case object MustExpand

final case class Next(val block: Array[AnyRef]) {
  @volatile var index: Int = 0
}

final class CallbackElem[-T, S] (
  val folder: (S, T) => S,
  val finalizer: (Int, S) => Any,
  val next: CallbackList[T],
  var block: Array[AnyRef],
  var position: Int,
  @volatile var accumulator: S
) extends CallbackList[T] {
  @volatile var lock: Int = -1
  var done: Boolean = false
  
  def copied = new CallbackElem(folder, finalizer, next, block, position, accumulator)
  
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
        if (!done)
          FlowPool.task(new CallbackElem.BatchTask(this))
      } else awakeCallback()
    }
  }
  
  def tryOwn(): Boolean = CallbackElem.CAS_COUNT(this, -1, 1)
  
  def unOwn() = CallbackElem.WRITE_COUNT(this, -1)
}

object CallbackElem {
  
  val unsafe = getUnsafe()
  
  val COUNTOFFSET = unsafe.objectFieldOffset(classOf[CallbackElem[_, _]].getDeclaredField("lock"))
  
  def CAS_COUNT(obj: CallbackElem[_, _], ov: Int, nv: Int) = unsafe.compareAndSwapInt(obj, COUNTOFFSET, ov, nv)
  
  def WRITE_COUNT(obj: CallbackElem[_, _], v: Int) = unsafe.putIntVolatile(obj, COUNTOFFSET, v)
  
  final class BatchTask[T, S](val callback: CallbackElem[T, S]) extends RecursiveAction {
    import FlowPool._
    
    // when entering this method, we have to hold the lock!
    @tailrec
    protected def compute() {
      if (callback.done) return

      if (callback.position >= LAST_CALLBACK_POS) {
        callback.block(MUST_EXPAND_POS) match {
          case MustExpand => // don't do anything
          case Next(b) =>
            callback.block = b
            callback.position = 0
        }
      }
      
      // invoke callback while there are elements or we reach an end of the block
      val block = callback.block
      var pos = callback.position
      var cur = block(pos)
      var acc = callback.accumulator
      while (!cur.isInstanceOf[CallbackHolder[_]]) {
        //acc = callback.folder(acc, cur.asInstanceOf[T]) /* strange, but this slows down everything... dunno why, but don't change */
        callback.accumulator = callback.folder(callback.accumulator, cur.asInstanceOf[T])
        pos += 1
        cur = block(pos)
      }
      callback.position = pos
      //callback.accumulator = acc

      // Check for seal
      cur match {
        case Seal(sz, null) =>
          callback.finalizer(sz, callback.accumulator)
          callback.done = true
        case _ =>
      }
      
      // relinquish control
      callback.unOwn()

      if (callback.done) return
      
      // see if there are more elements available
      // if there are, try to regain control and start again

      if (callback.position >= LAST_CALLBACK_POS) {
        block(MUST_EXPAND_POS) match {
          case MustExpand => // done
          case Next(b) => if (callback.tryOwn()) {
            callback.block = b
            callback.position = 0
            FlowPool.task(new CallbackElem.BatchTask(callback))
          }
        }
      } else {
        cur = block(pos)
        cur match {
          case Seal(sz, null) =>
            // Shortcut
            if (callback.tryOwn()) {
              callback.finalizer(sz, callback.accumulator)
              callback.done = true
              callback.unOwn()
            }
          case _: CallbackHolder[_] =>
          case _ => if (callback.tryOwn()) { compute() }
        }
      }
    }
  }
  
}
