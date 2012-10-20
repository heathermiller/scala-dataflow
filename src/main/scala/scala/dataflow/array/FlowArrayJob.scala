package scala.dataflow.array

import scala.annotation.tailrec

private[array] class FlowArrayJob(j: () => Unit) extends Function0[Unit] {

  import FlowArrayJob._

  // Job list management
  private var head: JobElem = JobElem(j)
  private var tail: JobElem = head

  // Utility
  private val unsafe = getUnsafe()
  private val NEXTOFFSET = unsafe.objectFieldOffset(classOf[JobElem].getDeclaredField("next"))
  @inline private def CAS_NEXT(el: JobElem, ov: JobList, nv: JobList) =
    unsafe.compareAndSwapObject(el, NEXTOFFSET, ov, nv)

  override def apply() {
    
    // TODO implement

  }

  /**
   * Adds a taks to this job
   * @return true on success, false if job terminated
   */
  def add(j: () => Unit): Boolean = {
    val nv = JobElem(j)
    @tailrec
    def add0(cur: JobElem): Boolean = {
      val ov = /*READ*/cur.next 
      ov match {
        case Done => false
        case je: JobElem =>
          tail/*WRITE*/ = je
          add0(je)
        case Free =>
          if (CAS_NEXT(cur, ov, nv)) true
          else add0(cur)
      }
    }
    add0(/*READ*/tail)
  }
}

object FlowArrayJob {

  sealed abstract class JobList
  case object Done extends JobList
  case object Free extends JobList
  case class JobElem(j: () => Unit, var next: JobList = Free) extends JobList

  def apply(j: () => Unit) = {
    val job = new FlowArrayJob(j)

    // TODO schedule
    
    job
  }

}
