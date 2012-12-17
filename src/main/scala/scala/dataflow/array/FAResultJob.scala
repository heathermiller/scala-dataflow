package scala.dataflow.array

abstract private[array] class FAResultJob[A] (
  start: Int,
  end: Int,
  thr: Int,
  obs: FAJob.Observer
) extends FAJob(start, end, thr, obs) {

  override protected type SubJob <: FAResultJob[A]

  @volatile
  private var result: Option[A] = None

  protected def setResult(v: A) {
    result = Some(v)
  }

  protected def combineResults(x: A, y: A): A

  final def getResult = result.get

  override def done = !result.isEmpty && super.done

  protected override def covers(from: Int, to: Int) = false

  override def jobDone() {
    if (super.done && isSplit) {
      val (j1, j2) = subTasks
      setResult(combineResults(j1.result.get, j2.result.get))
    }
    super.jobDone()
  }

}
