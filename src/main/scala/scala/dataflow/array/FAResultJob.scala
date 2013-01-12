package scala.dataflow.array

/**
 * Abstract super class for any FAJob that yields a single result.
 *
 * Handles combination of results when sub-jobs have completed
 */
abstract private[array] class FAResultJob[A] (
  start: Int,
  end: Int,
  thr: Int,
  obs: FAJob.Observer
) extends FAJob(start, end, thr, obs) {

  override protected type SubJob <: FAResultJob[A]

  /** result of this job */
  @volatile
  private var result: Option[A] = None

  /** use in sub class to set result upon completion */
  protected final def setResult(v: A) {
    result = Some(v)
  }

  /** override in sub class to combine two results */
  protected def combineResults(x: A, y: A): A

  /**
   * retrieve result
   * @return result of this FAJob
   * @throws exception if this FAJob is not completed yet
   */
  final def getResult = result.get

  override def done = !result.isEmpty && super.done

  /** result jobs do not cover anything */
  override protected def covers(from: Int, to: Int) = false

  override def jobDone() {
    if (super.done && isSplit) {
      val (j1, j2) = subTasks
      setResult(combineResults(j1.result.get, j2.result.get))
    }
    super.jobDone()
  }

}
