package scala.dataflow.array

import scala.dataflow.Future

private[array] class FAFoldJob[A : ClassManifest, A1 >: A] private (
  val src: FlatFlowArray[A],
  val trg: Future[A1],
  val z: A1,
  val f: (A1, A1) => A1,
  start: Int,
  end: Int,
  thr: Int,
  obs: FAJob.Observer
) extends FAJob(start, end, thr, obs) {

  override protected type SubJob = FAFoldJob[A,A1]

  @volatile private var result: Option[A1] = None

  protected def subCopy(s: Int, e: Int) = 
    new FAFoldJob(src, null, z, f, s, e, thresh, this)

  protected def doCompute() {
    var tmp = z
    for (i <- start to end) {
      tmp = f(tmp, src.data(i))
    }
    result = Some(tmp)
  }

  override def done = !result.isEmpty && super.done

  override def jobDone() {
    if (super.done && isSplit) {
      val (j1, j2) = subTasks
      result = Some(f(j1.result.get, j2.result.get))
      if (trg != null) trg.tryComplete(result.get)
    }
    super.jobDone()
  }

  protected override def covers(from: Int, to: Int) = false

}

object FAFoldJob {

  import FAJob.JobGen

  def apply[A : ClassManifest, A1 >: A](
    trg: Future[A1],
    z: A1,
    f: (A1, A1) => A1
  ) = new JobGen[A] {
    def apply(src: FlatFlowArray[A], dstOffset: Int, srcOffset: Int, length: Int) =
      new FAFoldJob(src, trg, z, f, srcOffset,
                    srcOffset + length - 1, FAJob.threshold(length), null)
  }

}
