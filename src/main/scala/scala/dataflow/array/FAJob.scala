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
      statRecLen(size)
      doCompute()
      finalizeCompute()
    }
  }

  @tailrec
  final protected def finalizeCompute(): Unit = /*READ*/state match {
    case Delegated(delegs, _) =>
      delegs.foreach(_.addObserver(this))
      // Prevent races
      if (delegs.forall(_.done))
        notifyObservers()
    case PendingChain(next) =>
      state = /*WRITE*/DoneChain(next)
      notifyObservers()
      next.fork()
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
    if (done) {
      if (popDelegate())
        // Work down the dependency chain, and notify observers
        finalizeCompute()
      else
        notifyObservers()
    }
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
  def done: Boolean = /*READ*/state match {
    case Split(j1, j2) =>
      j1.done && j2.done
    case DoneChain(_) | DoneEnd => true
    case Delegated(delegs, _) => delegs.forall(_.done)
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
   * Delegates this job some other jobs. MUST NOT be called elsewhere than
   * in thedoCompute body of a concrete subclass. Delegating has the
   * following effects: 
   * 1) This job's dependency list is kept but delayed until all
   *    delegated jobs complete.
   * 2) Future calls to done are proxied to the delegates
   *    (done in pattern matching in done method)
   * 3) This job's observers are only notified, once the delegates
   *    completes. (done in finalizeCompute)
   * 
   * Note that this method does *not* schedule the delegates.
   */
  @tailrec
  final protected def delegate(deleg: Seq[FAJob]) {
    /*READ*/state match {
      case cs@PendingChain(next) => 
        state/*WRITE*/ = Delegated(deleg, cs)
      case PendingFree => 
        if (!CAS_ST(PendingFree, Delegated(deleg, PendingFree)))
          delegate(deleg)
      case _ => throw new IllegalStateException("Delegate called while not executing.")
    }
  }

  final private def popDelegate(): Boolean = /*READ*/state match {
    case ov@Delegated(_, cs) =>
      if (!CAS_ST(ov, cs))
        popDelegate()
      else
        true
    case _ => false
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
        case Delegated(_, PendingChain(next)) =>
          dep0(next, newJob)
        case ov@Delegated(delegs, PendingFree) =>
          if (!CAS_ST(ov, Delegated(delegs, PendingChain(newJob))))
            dep0(cur, newJob)
          else
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

  import java.util.concurrent.atomic.AtomicInteger

  val statCount    = new AtomicInteger(0)
  val statCumSize  = new AtomicInteger(0)

  private def statRecLen(size: Int) {
    statCount.incrementAndGet()
    statCumSize.addAndGet(size)
  }

  def printStats() = {
    val count = statCount.get
    val len   = statCumSize.get.toDouble / count
    println("Computed %d jobs with %.2f average length".format(count, len))
  }

  sealed abstract class State
  sealed abstract class ChainState extends State
  case class Splitting(j1: FAJob, j2: FAJob, next: FAJob) extends State
  case class Split(j1: FAJob, j2: FAJob) extends State
  case class PendingChain(next: FAJob) extends ChainState
  case class DoneChain(next: FAJob) extends State
  case class Delegated(deleg: Seq[FAJob], oldState: ChainState) extends State
  case object PendingFree extends ChainState
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
    math.max(256,
      scala.collection.parallel.thresholdFromSize(
      size, scala.collection.parallel.availableProcessors
    ))
  )

  trait Observer {
    /**
     * called at least once when the observed job is done.
     * MUST be idempotent!
     */
    def jobDone() {}
  }

}
