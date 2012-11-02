package scala.dataflow.array

import scala.annotation.tailrec
import jsr166y._

private[array] abstract class FAJob(
  val start:  Int,
  val end:    Int,
  val thresh: Int,
  protected var observer: FAJob.Observer
) extends RecursiveAction with FAJob.Observer {

  import FAJob._

  /// FAJob internals ///

  /** state of this FAJob (done/pending/split/chained) */
  @volatile private var state: State = PendingFree

  private val unsafe = getUnsafe()
  private val OFFSET = unsafe.objectFieldOffset(classOf[FAJob].getDeclaredField("state"))
  @inline private def CAS(ov: State, nv: State) =
    unsafe.compareAndSwapObject(this, OFFSET, ov, nv)

  /** set the next job of this FAJob
   *
   * used while splitting dependent tasks
   */
  @inline private def setNext(next: FAJob): Unit =
    CAS(PendingFree, PendingChain(next))

  /***************************/
  /* Abstract Members        */
  /***************************/

  protected def subCopy(start: Int, end: Int): FAJob

  protected def subJobs: (FAJob, FAJob) = {
    val ((s1, e1), (s2, e2)) = splitInds
    
    (subCopy(s1, e1), subCopy(s2, e2))
  }

  protected def doCompute(): Unit

  /***************************/
  /* Helpers                 */
  /***************************/

  /** indices to handle after split */ 
  @inline protected final def splitInds = {
    val mid = start + size / 2
    ((start, mid),(mid + 1, end))
  }

  /** Number of elements this job handles */
  val size = end - start + 1

  /** checks if this job still needs splitting */
  @inline final def needSplit = size > thresh

  /** returns both subtasks (exception if not split) */
  final protected def subTasks = state match {
    case Split(j1, j2) => (j1, j2)
    case _ => throw new IllegalStateException("not split!")
  }


  /***************************/
  /* ForkJoinTask internals  */
  /***************************/

  final protected def compute() {
    if (needSplit) {
      val (j1, j2) = split()
      ForkJoinTask.invokeAll(j1, j2)
    } else {
      doCompute()
      finalizeCompute()
    }
  }

  @tailrec
  final private def finalizeCompute(): Unit = /*READ*/state match {
    case PendingChain(next: FAJob) =>
      state = /*WRITE*/DoneChain(next)
      notifyObservers()
      next.fork()
    case PendingFree =>
      if (!CAS(PendingFree, DoneEnd))
        finalizeCompute()
      else
        notifyObservers()
    case _ =>
      throw new IllegalStateException("Concurrent invocations")
  }

  /***************************/
  /* Done signaling          */
  /***************************/
  override def jobDone() {
    if (done) notifyObservers()
  }

  protected def notifyObservers() {
    val obs = /*READ*/observer
    if (obs != null) {
      obs.jobDone()
      observer/*WRITE*/ = null
    }
  }

  /// Public Members ///

  /** Checks whether this Job is done */
  def done: Boolean = state match {
    case Split(j1, j2) =>
      j1.done && j2.done
    case DoneChain(_) | DoneEnd => true
    case _ => false
    // Note: If state is splitting, there IS a next job which is not
    // yet started (otherwise: not splitting)
  }

  /***************************/
  /* Dependency / Split      */
  /***************************/

  /**
   * Splits this task in two subtasks (including chained tasks)
   * @return tuple with subtasks
   */
  final private def split(): (FAJob, FAJob) = {
    // Stupid workaround for tail-rec
    @tailrec
    def split0(): (FAJob, FAJob) = {
      val ov = /*READ*/state
      ov match {
        case PendingFree => 
          val sj = subJobs
          // State of subJobs is already PendingFree
          if (!CAS(ov, Split(sj._1, sj._2))) split0()
          else sj
        case PendingChain(next) =>
          val sj = subJobs
          CAS(ov, Splitting(sj._1, sj._2, next))
          split0()
        case Splitting(j1, j2, next) => 
          val (nj1, nj2) = next.split()
          j1.setNext(nj1)
          j2.setNext(nj2)
          CAS(ov, Split(j1, j2))
          (j1,j2)
        case Split(j1, j2) => (j1, j2)
        case _ =>
          throw new IllegalStateException("Split called on FAJob after work started")
      }
    }

    split0()
  }

  /**
   * Submits a depending task for execution: Schedules if this one is done, chains if this
   * one hasn't yet finished.
   * 
   * @return true on success, false otherwise
   */
  final def depending(newJob: FAJob) {
    @tailrec
    def dep0(cur: FAJob, newJob: FAJob): Option[(FAJob, FAJob)] = {
      /*READ*/cur.state match {
        case _: Splitting =>
          cur.split()
          dep0(cur, newJob)
        case Split(j1, j2) =>
          Some((j1, j2))
        case PendingChain(next) =>
          dep0(next, newJob)
        case DoneChain(next) =>
          dep0(next, newJob)
        case PendingFree =>
          if (!CAS(PendingFree, PendingChain(newJob)))
            dep0(cur, newJob)
          else
            None
        case DoneEnd =>
          schedule(newJob)
          None
      }
    }
    
    dep0(this, newJob) map {
      case (j1, j2) =>
        val (nj1, nj2) = newJob.split()
        j1.depending(nj1)
        j2.depending(nj2)
    }
  }

}

object FAJob {

  sealed abstract class State
  case class Splitting(j1: FAJob, j2: FAJob, next: FAJob) extends State
  case class Split(j1: FAJob, j2: FAJob) extends State
  case class PendingChain(next: FAJob) extends State
  case class DoneChain(next: FAJob) extends State
  case object PendingFree extends State
  case object DoneEnd extends State

  val forkjoinpool = new ForkJoinPool

  def schedule(job: FAJob) = Thread.currentThread match {
    case fjw: ForkJoinWorkerThread =>
      job.fork()
    case _ =>
      forkjoinpool.execute(job)
  }

  def threshold(size: Int) = (
    scala.collection.parallel.thresholdFromSize(
      size, scala.collection.parallel.availableProcessors
    )
  )

  trait Observer {
    def jobDone() {}
  }

}
