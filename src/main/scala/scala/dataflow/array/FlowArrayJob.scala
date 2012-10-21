package scala.dataflow.array

import scala.annotation.tailrec

import jsr166y._

final private[array] class FlowArrayJob(j: () => Unit) extends RecursiveAction {

  import FlowArrayJob._

  // Job list management
  private var head: JobElem = JobElem(j)
  @volatile private var tail: JobElem = head

  // Utility
  private val unsafe = getUnsafe()
  private val NEXTOFFSET = unsafe.objectFieldOffset(classOf[JobElem].getDeclaredField("next"))
  @inline private def CAS_NEXT(el: JobElem, ov: JobList, nv: JobList) =
    unsafe.compareAndSwapObject(el, NEXTOFFSET, ov, nv)

  @tailrec
  protected def compute() {

    // Do the work
    head.j()

    /*READ*/head.next match {
      case Done => sys.error("compute() called twice!")
      case Free =>
        if (!CAS_NEXT(head, Free, Done)) {
          /*READ*/head.next match {
            case je: JobElem =>
              head = je
              compute()
            case _ => sys.error("multiple threads for same JobElem!")
          }
        }
      case je: JobElem =>
        head = je
        compute()
    }

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

  val forkjoinpool = new ForkJoinPool
  
  private def task[T](fjt: ForkJoinTask[T]) = Thread.currentThread match {
    case fjw: ForkJoinWorkerThread =>
      fjt.fork()
    case _ =>
      forkjoinpool.execute(fjt)
  }

  def apply(j: () => Unit) = {
    val job = new FlowArrayJob(j)

    task(job)
    
    job
  }

}
