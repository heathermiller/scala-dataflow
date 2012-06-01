package scala.dataflow

import java.util.concurrent.atomic.AtomicInteger

final class MLHasher(lanes: Int) {
  val shift = new AtomicInteger(0)
  def advance = shift.incrementAndGet()
  def getblocki(tid: Long) = {
    val tmp = (tid.asInstanceOf[Int]+shift.get()) * 0x9e3775cd
    math.abs((java.lang.Integer.reverseBytes(tmp) * 0x9e3775cd) % lanes)
  }
}
