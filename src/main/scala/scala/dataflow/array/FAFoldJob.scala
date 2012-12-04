package scala.dataflow.array

import scala.dataflow.Future

private[array] class FAFoldJob[A : ClassManifest, A1 >: A] private (
  val src: FlatFlowArray[A],
  val z: A1,
  val f: (A1, A1) => A1,
  val g: A => A1,
  start: Int,
  end: Int,
  thr: Int,
  obs: FAJob.Observer
) extends FAJob(start, end, thr, obs) {

  override protected type SubJob = FAFoldJob[A,A1]

  @volatile private var result: Option[A1] = None

  protected def subCopy(s: Int, e: Int) = 
    new FAFoldJob(src, z, f, g, s, e, thresh, this)

  protected def doCompute() {
    var tmp = z
    for (i <- start to end) {
      tmp = f(tmp, g(src.data(i)))
    }
    result = Some(tmp)
  }

  final def getResult = result.get

  override def done = !result.isEmpty && super.done

  override def jobDone() {
    if (super.done && isSplit) {
      val (j1, j2) = subTasks
      result = Some(f(j1.result.get, j2.result.get))
    }
    super.jobDone()
  }

  protected override def covers(from: Int, to: Int) = false

}

object FAFoldJob {

  import FAJob.JobGen

  def apply[A : ClassManifest, A1 >: A](
    src: FlatFlowArray[A],
    fut: FoldFuture[A1],
    srcOffset: Int,
    length: Int,
    z: A1,
    f: (A1, A1) => A1,
    g: A => A1
  ): FAFoldJob[A,A1] = {
    val job = new FAFoldJob(src, z, f, g, srcOffset,
                            srcOffset + length - 1, FAJob.threshold(length), null)
    fut.setJob(job)
    job.addObserver(fut)
    job
  }

  def apply[A : ClassManifest, A1 >: A](
    src: FlatFlowArray[A],
    fut: FoldFuture[A1],
    srcOffset: Int,
    length: Int,
    z: A1,
    f: (A1, A1) => A1
  ): FAFoldJob[A,A1] = FAFoldJob(src, fut, srcOffset, length, z, f, (x: A) => x)

}
