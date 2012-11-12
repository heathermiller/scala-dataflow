package scala.dataflow.array

import scala.annotation.tailrec
import jsr166y._

private[array] abstract class FAJob(
  val start:  Int,
  val end:    Int,
  val thresh: Int,
  observer: FAJob.Observer
) extends RecursiveAction with FAJob.Observer {

  import FAJob._

  /// FAJob internals ///

  /** observers of this FAJob */
  @volatile private var observers: ObsStack = 
    if (observer == null) ObsBot else ObsEl(observer)

  /** state of this FAJob (done/pending/split/chained) */
  @volatile private var state: State = PendingFree

  private val unsafe = getUnsafe()
  private val STATE_OFFSET =
    unsafe.objectFieldOffset(classOf[FAJob].getDeclaredField("state"))
  @inline private def CAS_ST(ov: State, nv: State) =
    unsafe.compareAndSwapObject(this, STATE_OFFSET, ov, nv)
  private val OBS_OFFSET =
    unsafe.objectFieldOffset(classOf[FAJob].getDeclaredField("observers"))
  @inline private def CAS_OB(ov: ObsStack, nv: ObsStack) =
    unsafe.compareAndSwapObject(this, OBS_OFFSET, ov, nv)

  /** set the next job of this FAJob
   *
   * used while splitting dependent tasks
   */
  @inline private def setNext(next: FAJob): Unit =
    CAS_ST(PendingFree, PendingChain(next))

  /***************************/
  /* Abstract Members        */
  /***************************/

  protected def subCopy(start: Int, end: Int): FAJob

  protected def subJobs: (FAJob, FAJob) = {
    val ((s1, e1), (s2, e2)) = splitInds
    
    (subCopy(s1, e1), subCopy(s2, e2))
  }

  protected val autoFinalize: Boolean = true
  protected def doCompute(): Unit

  /***************************/
  /* Helpers                 */
  /***************************/

  /** indices to handle after split */ 
  @inline protected final def splitInds = {
    val mid = start + size / 2
    ((start, mid - 1),(mid, end))
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
      if (autoFinalize)
        finalizeCompute()
    }
  }

  @tailrec
  final protected def finalizeCompute(): Unit = /*READ*/state match {
    case Delegated(deleg) =>
      deleg.addObserver(this)
      // Prevent races
      if (deleg.done)
        notifyObservers()
    case PendingChain(next) =>
      state = /*WRITE*/DoneChain(next)
      notifyObservers()
      if (autoFinalize)
        next.fork()
      else
        schedule(next)
    case PendingFree =>
      if (!CAS_ST(PendingFree, DoneEnd))
        finalizeCompute()
      else
        notifyObservers()
    case _ =>
      // This can happen in cases when paused jobs (see flatMap) call finalizeCompute
      // in the jobDone callback. (which can be called multiple times)
  }

  /***************************/
  /* Done signaling          */
  /***************************/
  override def jobDone() {
    if (done) notifyObservers()
  }

  final protected def notifyObservers() {
    @tailrec
    def not0(cur: ObsStack): Unit = cur match {
      case ObsEl(obs, next) =>
        obs.jobDone()
        not0(next)
      case ObsBot =>
    }
    not0(/*READ*/observers)

    // Free stuff
    observers/*WRITE*/ = ObsBot
  }

  @tailrec
  final def addObserver(obs: Observer) {
    val ov = /*READ*/observers
    val nv = ObsEl(obs, ov)
    if (!CAS_OB(ov, nv))
      addObserver(obs)
  }

  /// Public Members ///

  /** Checks whether this Job is done */
  def done: Boolean = state match {
    case Split(j1, j2) =>
      j1.done && j2.done
    case DoneChain(_) | DoneEnd => true
    case Delegated(deleg) => deleg.done
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
          if (!CAS_ST(ov, Split(sj._1, sj._2))) split0()
          else sj
        case PendingChain(next) =>
          val sj = subJobs
          CAS_ST(ov, Splitting(sj._1, sj._2, next))
          split0()
        case Splitting(j1, j2, next) => 
          val (nj1, nj2) = next.split()
          j1.setNext(nj1)
          j2.setNext(nj2)
          CAS_ST(ov, Split(j1, j2))
          (j1,j2)
        case Split(j1, j2) => (j1, j2)
        case _ =>
          throw new IllegalStateException("Split called on FAJob after work started")
      }
    }

    split0()
  }

  /**
   * Delegates this job to another job. Should be called in the
   * doCompute body of a concrete subclass. Delegating has the
   * following effects: 
   * 1) This job's dependency list is moved downstream the delegated
   *    job's dependency list (actually done in this method)
   * 2) Future calls to depend and done are proxied to the delegate
   *    (done in pattern matching in the two methods)
   * 3) This job's observers are only notified, once the delegate
   *    completes. (done in finalizeCompute)
   * 
   * Note that this method does *not* schedule the delegate.
   */
  @tailrec
  final protected def delegate(deleg: FAJob) {
    /*READ*/state match {
      case PendingChain(next) => 
        deleg.depending(next)
        state/*WRITE*/ = Delegated(deleg)
      case PendingFree => 
        if (!CAS_ST(PendingFree, Delegated(deleg)))
          delegate(deleg)
      case _ => throw new IllegalStateException("Delegate called while not executing.")
    }
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
        case Delegated(deleg) =>
          deleg.depending(newJob)
          None
        case Split(j1, j2) =>
          Some((j1, j2))
        case PendingChain(next) =>
          dep0(next, newJob)
        case DoneChain(next) =>
          dep0(next, newJob)
        case PendingFree =>
          if (!CAS_ST(PendingFree, PendingChain(newJob)))
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
  case class Delegated(deleg: FAJob) extends State
  case object PendingFree extends State
  case object DoneEnd extends State

  sealed abstract class ObsStack
  case class ObsEl(cur: Observer, n: ObsStack = ObsBot) extends ObsStack
  case object ObsBot extends ObsStack

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
    /**
     * called at least once when the observed job is done.
     * MUST be idempotent!
     */
    def jobDone() {}
  }

}
