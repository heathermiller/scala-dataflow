package scala.dataflow.array

import scala.annotation.tailrec
import jsr166y._

private[array] abstract class FAJob(
  val start:  Int,
  val end:    Int,
  val thresh: Int,
  observer: FAJob.Observer
) extends RecursiveAction with FAJob.Observer with SlicedJob {

  import FAJob._

  /// FAJob internals ///

  /** observers of this FAJob */
  @volatile private var observers: ObsStack = 
    if (observer == null) ObsEmpty else ObsEl(observer)

  /** state of this FAJob (done/pending/split/chained) */
  @volatile private var state: State[SubJob] = PendingFree

  private val unsafe = getUnsafe()
  private val STATE_OFFSET =
    unsafe.objectFieldOffset(classOf[FAJob].getDeclaredField("state"))
  @inline private def CAS_ST(ov: State[_], nv: State[SubJob]) =
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

  protected type SubJob <: FAJob

  protected def subCopy(start: Int, end: Int): SubJob

  protected def subJobs: (SubJob, SubJob) = {
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

  final protected def isSplit = /*READ*/state match {
    case Split(_, _) => true
    case _ => false
  }

  final protected def isDelegated = /*READ*/state match {
    case Delegated(_, _, _) => true
    case _ => false
  }

  final protected def delegates = /*READ*/state match {
    case Delegated(delegs, _, _) => delegs
    case _ => throw new IllegalStateException("not delegated!")
  }

  /***************************/
  /* ForkJoinTask internals  */
  /***************************/

  final protected def compute() {
    try {
      if (needSplit) {
        val (j1, j2) = split()
        ForkJoinTask.invokeAll(j1, j2)
      } else {
        statRecLen(size)
        try {
          doCompute()
        } catch {
          // TODO catch any throwable, expose it to user!
          case e: Exception =>
            println("Inner")
            e.printStackTrace()
        }
        finalizeCompute()
      }
    } catch {
      case e: Exception =>
        println("Fatal")
        e.printStackTrace()
    }
  }

  @tailrec
  final private def finalizeCompute(): Unit = /*READ*/state match {
    case d@Delegated(delegs, _, _) =>
      d.setObs(this)
      // Prevent races
      if (d.done) jobDone()
    case ov@PendingChain(next) =>
      if (!CAS_ST(ov, Done))
        finalizeCompute()
      else {
        notifyObservers()
        next.fork()
      }
    case PendingFree =>
      if (!CAS_ST(PendingFree, Done))
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
    @tailrec
    def done0(): Unit = /*READ*/state match {
      // We are notified by a delegate
      case ov@Delegated(_, cs, then) if ov.done =>
        if (!CAS_ST(ov, cs)) done0()
        else {
          if (then != null) { then() }
          // Work down the dependency chain, and notify observers
          finalizeCompute()
        }

      // We are notified by a subjob
      case Split(j1, j2) if j1.done && j2.done =>
        notifyObservers()
      case _ =>
    }
    done0()
  }

  @tailrec
  final private def notifyObservers() {
    val ov = /*READ*/observers
    if (CAS_OB(ov, ObsNotified))
      ov.jobDone()
    else
      notifyObservers()
  }

  @tailrec
  final def tryAddObserver(obs: Observer): Boolean = /*READ*/observers match {
    case ObsNotified => false
    case ov => 
      val nv = ObsEl(obs, ov)
      if (!CAS_OB(ov, nv))
        tryAddObserver(obs)
      else
        true
  }

  final def addObserver(obs: Observer) {
    if (!tryAddObserver(obs)) obs.jobDone()
  }

  /// Public Members ///

  /** Checks whether this Job is done */
  def done: Boolean = /*READ*/state match {
    case Split(j1, j2) =>
      j1.done && j2.done
    case Done => true
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
    def split0(): (SubJob, SubJob) = {
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
   * in the doCompute body of a concrete subclass. Delegating has the
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
  final protected def delegate(deleg: IndexedSeq[FAJob]) = delegateThen(deleg)(null)

  @tailrec
  final protected def delegateThen(deleg: IndexedSeq[FAJob])(then: () => Unit) {
    /*READ*/state match {
      case ov: ChainState[_] =>
        if (!CAS_ST(ov, Delegated(deleg, ov, then)))
          delegateThen(deleg)(then)
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
        case _: Splitting[_] =>
          cur.split()
          dep0(cur, newJob)
        case Delegated(_, PendingChain(next), _) =>
          dep0(next, newJob)
        case ov@Delegated(delegs, PendingFree, then) =>
          if (!CAS_ST(ov, Delegated(delegs, PendingChain(newJob), then)))
            dep0(cur, newJob)
          else
            None
        case Split(j1, j2) =>
          Some((j1, j2))
        case PendingChain(next) =>
          dep0(next, newJob)
        case PendingFree =>
          if (!CAS_ST(PendingFree, PendingChain(newJob)))
            dep0(cur, newJob)
          else
            None
        case Done =>
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

  /**
   * Returns the smallest job responsible for this particular slice in
   * destination FA indices. Uses covers in sub-class.
   */ 
  final protected def destSliceJob(from: Int, to: Int) = {
    @tailrec
    def dsj0(cur: FAJob): FAJob = /*READ*/cur.state match {
      case _: Splitting[_] =>
        // Help splitting
        cur.split()
        dsj0(cur)
      case Split(j, _) if j.covers(from, to) => dsj0(j)
      case Split(_, j) if j.covers(from, to) => dsj0(j)
      case _ => cur
    }
    dsj0(this)
  }

  /**
   * Returns the set of smallest jobs responsible for this particular
   * slice. Should be overriden by jobs where destSliceJob does not make sense.
   */
  def destSliceJobs(from: Int, to: Int) = Vector(destSliceJob(from, to))

  /** adapter for extended interface (with repeat information) */
  private[array] def sliceJobs(from: Int, to: Int) = {
    val js = destSliceJobs(from, to).filterNot(_.done)
    if (js.isEmpty) None
    else Some(js, false)
  }

  /**
   * whether this job entirely covers the given slice
   */
  protected def covers(from: Int, to: Int) =
    from >= start && to <= end

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

  def resetStats() {
    statCount.set(0)
    statCumSize.set(0)
  }

  sealed abstract class State[+S <: FAJob]
  sealed abstract class ChainState[+S <:FAJob] extends State[S]
  case class Splitting[+S <: FAJob](j1: S, j2: S, next: FAJob) extends State[S]
  case class Split[+S <: FAJob](j1: S, j2: S) extends State[S]
  case class PendingChain(next: FAJob) extends ChainState[Nothing]
  case class Delegated[+S <: FAJob](
      deleg: IndexedSeq[FAJob],
      oldState: ChainState[S],
      then: () => Unit = null)
    extends State[S] with Observer {
  
    @volatile var doneInd: Int = 0
    @volatile var obs: Observer = null

    final def setObs(o: Observer) {
      obs = o
      deleg(0).addObserver(this)
    }
    final def done = advDone() >= deleg.size
    @tailrec
    final override def jobDone() {
      if (done) { 
        val o = /*READ*/obs
        if (o != null) o.jobDone()
      } else if (!deleg(doneInd).tryAddObserver(this))
        jobDone()
    }
    private def advDone() = {
      var i = /*READ*/doneInd
      while (i < deleg.size && deleg(i).done) { i += 1 }
      doneInd/*WRITE*/ = i
      i
    }
  }

  case object PendingFree extends ChainState[Nothing]
  case object Done extends State[Nothing]

  sealed abstract class ObsStack extends Observer
  case class ObsEl(cur: Observer, n: ObsStack = ObsEmpty) extends ObsStack {
    override final def jobDone() { cur.jobDone(); n.jobDone() }
  }
  case object ObsEmpty extends ObsStack
  case object ObsNotified extends ObsStack

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

  trait JobGen[A] extends Function4[FlatFlowArray[A], Int, Int, Int, FAJob] {
    def apply(src: FlatFlowArray[A], dstOffset: Int, srcOffset: Int, length: Int): FAJob
  }

  trait Observer {
    /**
     * called at least once when the observed job is done.
     * MUST be idempotent!
     */
    def jobDone() {}
  }

}
