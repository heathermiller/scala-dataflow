package scala.dataflow.array

import scala.annotation.tailrec
import jsr166y._

/**
 * Abstract superclass of all Jobs used for splitting and dependency
 * tracking.
 *
 * FAJobs are automatically split, until their size is below the
 * threshold.
 */
private[array] abstract class FAJob(
  /** start of the slice this FAJob works on */
  val start:  Int,
  /** end (incl) of the slice this FAJob works on */
  val end:    Int,
  /** splitting threshold */
  val thresh: Int,
  observer: FAJob.Observer
) extends RecursiveAction with FAJob.Observer with SlicedJob {

  import FAJob._

  /// Record stats ///
  statNewJob(this.getClass)

  /// FAJob internals ///

  /** observers of this FAJob */
  @volatile
  private var observers: ObsStack = 
    if (observer == null) ObsEmpty else ObsEl(observer)

  /** state of this FAJob (done/pending/split/chained) */
  @volatile
  private var state: State[SubJob] = PendingFree

  /** convenience method to CAS state */
  @inline
  private def CAS_ST(ov: State[_], nv: State[SubJob]) =
    unsafe.compareAndSwapObject(this, STATE_OFFSET, ov, nv)
  /** convenience method to CAS observer stack */
  @inline
  private def CAS_OB(ov: ObsStack, nv: ObsStack) =
    unsafe.compareAndSwapObject(this, OBS_OFFSET, ov, nv)

  /** 
   * set the next job of this FAJob
   * 
   * used while splitting dependent tasks
   * @param next the next job in the dependency queue
   */
  @inline
  private def setNext(next: FAJob): Unit =
    CAS_ST(PendingFree, PendingChain(next))

  /***************************/
  /* Abstract Members        */
  /***************************/

  /** type of job this FAJob generates when splitting */
  protected type SubJob <: FAJob

  /**
   * returns a copy of this type of FAJob, working on the given range
   */
  protected def subCopy(start: Int, end: Int): SubJob

  /** actual computation of this job */
  protected def doCompute(): Unit

  /***************************/
  /* Helpers                 */
  /***************************/

  /**
   * returns two sub jobs of this job
   *
   * uses `subCopy`, used when splitting
   * @return tuple with two jobs
   */
  protected def subJobs: (SubJob, SubJob) = {
    val ((s1, e1), (s2, e2)) = splitInds
    
    (subCopy(s1, e1), subCopy(s2, e2))
  }

  /**
   * calculate indices to handle after split
   *
   * separates the current range in two ranges that cover it.
   * @return tuple of two ranges
   */ 
  @inline protected final def splitInds = {
    val mid = start + size / 2
    ((start, mid - 1),(mid, end))
  }

  /** Number of elements this job handles */
  val size = end - start + 1

  /** checks if this job still needs splitting */
  @inline
  final def needSplit = size > thresh

  /** returns both subtasks (exception if this FAJob is not split) */
  final protected def subTasks = state match {
    case Split(j1, j2) => (j1, j2)
    case _ => throw new IllegalStateException("not split!")
  }

  /** checks if this FAJob is split */
  final protected def isSplit = /*READ*/state match {
    case Split(_, _) => true
    case _ => false
  }

  /**
   * checks if this FAJob is delegated, i.e if calculation is halted
   * until some other jobs complete
   */
  final protected def isDelegated = /*READ*/state match {
    case Delegated(_, _, _) => true
    case _ => false
  }

  /**
   * optionally fetches the delegates of this FAJob.
   *
   * @return `Some(delegs)` if this FAJob is delegated, `None`
   *         otherwise
   */
  final protected def delegates = /*READ*/state match {
    case Delegated(delegs, _, _) => Some(delegs)
    case _ => None
  }

  /***************************/
  /* ForkJoinTask internals  */
  /***************************/

  /**
   * compute method of the ForkJoinTask.
   *
   * Splits this job if it is too big, launches computation otherwise
   */
  override protected final def compute() {
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

  /**
   * finalizes computation of this FAJob.
   *
   * called at the end of the doCompute method, but also each time
   * when `delegateThen` callbacks are invoked.
   */
  @tailrec
  private final def finalizeCompute(): Unit = /*READ*/state match {
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

  /**
   * called when a watched job is done.
   *
   * if this FAJob is delegated, this method pops the delegate and
   * executes the associated callback (if available). Then tries to
   * finalize again.
   *
   * if this FAJob is split, checks if observers need to be notified
   * (i.e. if other job is done already, too).
   */
  override def jobDone() {
    @tailrec
    def done0(): Unit = /*READ*/state match {
      // We are notified by a delegate
      case ov@Delegated(_, cs, thn) if ov.done =>
        if (!CAS_ST(ov, cs)) done0()
        else {
          if (thn != null) { thn() }
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

  /**
   * notifies all observers and sets internal state to notified
   */
  @tailrec
  private final def notifyObservers() {
    val ov = /*READ*/observers
    if (CAS_OB(ov, ObsNotified))
      // Whatever was there in ov, this is fine (might do nothing)
      ov.jobDone()
    else
      notifyObservers()
  }

  /**
   * Add observer to this FAJob.
   *
   * Add an observer to this FAJob which is notified once it completes
   * or return false, if this FAJob is already completed.
   *
   * This method is required to avoid unnecessary stack growth, where
   * tail recursion could have been used
   * 
   * @param obs Observer to add
   * @return true if the observer could be added, false if this FAJob
   *              is completed
   */
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

  /** Add observer to this FAJob */
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
   * Delegates this job to some other jobs. MUST NOT be called
   * elsewhere than in the doCompute body of a concrete
   * subclass. Delegating has the following effects: 
   * 1) This job's dependency list is kept but delayed until all
   *    delegated jobs complete.
   * 2) This job's observers are only notified, once the delegates
   *    completes. (done in finalizeCompute)
   * 
   * Note that this method does *not* schedule the delegates.
   */
  protected final def delegate(deleg: IndexedSeq[FAJob]) = delegateThen(deleg)(null)

  /**
   * Delegates this job to some other jobs and executes a callback
   * once the delegates complete. MUST NOT be called elsewhere than
   * in the doCompute body of a concrete subclass. Delegating has the
   * following effects: 
   * 1) This job's dependency list is kept.
   * 2) When the delegates complete, the `thn` callback continues
   *    execution of the doCompute method. I.e. delegateThen may be
   *    called again.
   * 3) When then `thn` callback completes and it as not delegated again,
   *    This job's observers are notified. (done in finalizeCompute)
   * 
   * Note that this method does *not* schedule the delegates.
   */
  @tailrec
  protected final def delegateThen(deleg: IndexedSeq[FAJob])(thn: () => Unit) {
    /*READ*/state match {
      case ov: ChainState[_] =>
        if (!CAS_ST(ov, Delegated(deleg, ov, thn)))
          delegateThen(deleg)(thn)
      case _ => throw new IllegalStateException("Delegate called while not executing.")
    }
  }

  /**
   * Submits a depending task for execution: Schedules if this one is done, chains if this
   * one hasn't yet finished.
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
        case ov@Delegated(delegs, PendingFree, thn) =>
          if (!CAS_ST(ov, Delegated(delegs, PendingChain(newJob), thn)))
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
  protected final def destSliceJob(from: Int, to: Int) = {
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

  override private[array] def sliceJobs(from: Int, to: Int) = {
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
  import java.util.concurrent.ConcurrentHashMap

  private val unsafe = getUnsafe()
  private val STATE_OFFSET =
    unsafe.objectFieldOffset(classOf[FAJob].getDeclaredField("state"))
  private val OBS_OFFSET =
    unsafe.objectFieldOffset(classOf[FAJob].getDeclaredField("observers"))

  /// Statistics ///

  val statCount    = new AtomicInteger(0)
  val statCumSize  = new AtomicInteger(0)
  val statMinSize  = new AtomicInteger(Integer.MAX_VALUE)
  val statMaxSize  = new AtomicInteger(0)
  val statJobTypes = new ConcurrentHashMap[String, AtomicInteger]()

  /** record statistics when a new FAJob is created */
  private def statNewJob(clazz: Class[_]) {
    val n = clazz.getName
    Option(statJobTypes.get(n)) map { ai => ai.incrementAndGet() } orElse {
      val nai = new AtomicInteger(1)
      Option(statJobTypes.putIfAbsent(n, nai)) map { ai => ai.incrementAndGet() }
    }
  }

  @tailrec
  private def updateMinSize(i: Int) {
    val tmp = statMinSize.get
    if (i < tmp)
      if (!statMinSize.compareAndSet(tmp, i))
        updateMinSize(i)
  }

  @tailrec
  private def updateMaxSize(i: Int) {
    val tmp = statMaxSize.get
    if (i > tmp)
      if (!statMaxSize.compareAndSet(tmp, i))
        updateMaxSize(i)
  }

  /** record statistics when a FAJob executes */
  private def statRecLen(size: Int) {
    statCount.incrementAndGet()
    statCumSize.addAndGet(size)
    updateMinSize(size)
    updateMaxSize(size)
  }

  def printStats() = {
    import scala.collection.JavaConversions._

    val count = statCount.get
    val len   = statCumSize.get.toDouble / count
    val max   = statMaxSize.get
    val min   = statMinSize.get
    println("Computed %d jobs with ".format(count))
    println("  average: %8.2f".format(len))
    println("  max:     %5d".format(max))
    println("  min:     %5d".format(min))

    statJobTypes.entrySet.foreach { e =>
      println("%-43s %6d instances".format(e.getKey, e.getValue.get))
    }
  }

  def resetStats() {
    statCount.set(0)
    statCumSize.set(0)
    statMinSize.set(Integer.MAX_VALUE)
    statMaxSize.set(0)
    statJobTypes.clear()
  }

  /** state of an FAJob */
  sealed abstract class State[+S <: FAJob]
  /**
   * any state that indicates that FAJob is pending and might have a
   * successor
   */
  sealed abstract class ChainState[+S <:FAJob] extends State[S]
  /** FAJob is being split */
  case class Splitting[+S <: FAJob](j1: S, j2: S, next: FAJob) extends State[S]
  /** FAJob is split */
  case class Split[+S <: FAJob](j1: S, j2: S) extends State[S]
  /** FAJob is not yet calculated, has successor */
  case class PendingChain(next: FAJob) extends ChainState[Nothing]
  /**
   * FAJob is delegated to some other jobs
   *
   * see `FAJob.delegate` and `FAJob.delegateThen`
   */ 
  case class Delegated[+S <: FAJob](
      deleg: IndexedSeq[FAJob],
      oldState: ChainState[S],
      thn: () => Unit = null)
    extends State[S] with Observer {

    @volatile
    var doneInd: Int = 0
    @volatile
    var obs: Observer = null

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

  /** FAJob is pending, no successor */
  case object PendingFree extends ChainState[Nothing]
  /** FAJob has finished calculating */
  case object Done extends State[Nothing]

  /** Observer stack for FAJob */
  sealed abstract class ObsStack extends Observer
  /** An Observer in the stack */
  case class ObsEl(cur: Observer, n: ObsStack = ObsEmpty) extends ObsStack {
    override final def jobDone() { cur.jobDone(); n.jobDone() }
  }
  /** No observer yet in this end of stack */
  case object ObsEmpty extends ObsStack
  /** All observers have been notified. Do not add new elements */
  case object ObsNotified extends ObsStack

  var parallelism: Int = scala.collection.parallel.availableProcessors
  lazy val forkjoinpool = new ForkJoinPool(parallelism)

  def setPar(n: Int) = {
    parallelism = n
  }

  /** schedule a FAJob for execution (unconditionally) */
  def schedule(job: FAJob) = Thread.currentThread match {
    case fjw: ForkJoinWorkerThread =>
      job.fork()
    case _ =>
      forkjoinpool.execute(job)
  }

  /** split threshold for a given original size */
  def threshold(size: Int) = (
    math.max(512,
      scala.collection.parallel.thresholdFromSize(size, parallelism)
           )
  )

  /**
   * FAJob generator. Used in FA dispatch framework.
   */
  trait JobGen[A] extends Function4[FlatFlowArray[A], Int, Int, Int, FAJob] {
    /**
     * If this returns true, FADispatcher jobs will not suppose that jobs will
     * generate results restricted to their own range. This influences the behavior of
     * the sliceJobs method.
     */
    def needDeepJobSearch: Boolean = false
 
    /**
     * creates a job with given source, offsets and lengths
     */
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
