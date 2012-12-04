package scala.dataflow.array

private[array] class FAZipMapJob[A : ClassManifest,
                                 B : ClassManifest,
                                 C : ClassManifest] private (
  val src: FlatFlowArray[A],
  val osrc: FlowArray[B],
  val dst: FlatFlowArray[C],
  val f: (A,B) => C,
  val dstOffset: Int,
  val oSrcOffset: Int,
  start: Int,
  end: Int,
  thr: Int,
  obs: FAJob.Observer
) extends FAJob(start, end, thr, obs) {

  override protected type SubJob = FAZipMapJob[A,B,C]

  protected def subCopy(s: Int, e: Int) = 
    new FAZipMapJob(src, osrc, dst, f, dstOffset, oSrcOffset, s, e, thresh, this)

  protected def doCompute() {
    osrc.sliceJobs(oSrcOffset + start, oSrcOffset + end) match {
      // no need to call sliceJobs again after completion
      case Some((j, false)) => delegateThen(j) { calculate _ }
      // required to call sliceJobs again after completion
      case Some((j, true))  => delegateThen(j) { doCompute _ }
      // None: osrc has finished!
      case None => calculate()
    }
  }

  private def calculate() {
    for (i <- start to end) {
      dst.data(i + dstOffset) = f(src.data(i), osrc.unsafe(i + oSrcOffset))
    }
  }

}

object FAZipMapJob {

  import FAJob.JobGen

  def apply[A : ClassManifest, B : ClassManifest, C : ClassManifest](
    osrc: FlowArray[B],
    dst: FlatFlowArray[C],
    f: (A,B) => C
  ) = new JobGen[A] {
    def apply(src: FlatFlowArray[A], dstOffset: Int, srcOffset: Int, length: Int) =
      new FAZipMapJob(src, osrc, dst, f, dstOffset - srcOffset, dstOffset - srcOffset,
                      srcOffset, srcOffset + length - 1, FAJob.threshold(length), null)
  }

}
