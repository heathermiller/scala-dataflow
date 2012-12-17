package scala.dataflow.array

private[array] class FAZipMapFoldJob[A : ClassManifest, B : ClassManifest, C] private (
  val src: FlatFlowArray[A],
  val osrc: FlowArray[B],
  val f: (A, B) => C,
  val z: C,
  val g: (C, C) => C,                                     
  val oSrcOffset: Int,
  start: Int,
  end: Int,
  thr: Int,
  obs: FAJob.Observer
) extends FAResultJob[C](start, end, thr, obs) {

  override protected type SubJob = FAZipMapFoldJob[A,B,C]

  protected def subCopy(s: Int, e: Int) = 
    new FAZipMapFoldJob(src, osrc, f, z, g, oSrcOffset, s, e, thresh, this)

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
    var tmp = z
    for (i <- start to end) {
      tmp = g(tmp, f(src.data(i), osrc.unsafe(i + oSrcOffset)))
    }
    setResult(tmp)
  }

  protected override def combineResults(x: C, y: C) = g(x,y)

}

object FAZipMapFoldJob {

  import FAJob.JobGen

  def apply[A : ClassManifest, B : ClassManifest, C](
    src: FlatFlowArray[A],
    osrc: FlowArray[B],
    f: (A,B) => C,
    z: C,
    g: (C,C) => C,
    srcOffset: Int,
    oSrcOffset: Int,
    length: Int
  ) = new FAZipMapFoldJob(src, osrc, f, z, g, oSrcOffset - srcOffset,
                          srcOffset, srcOffset + length - 1, FAJob.threshold(length), null)

}
