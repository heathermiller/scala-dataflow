package scala.dataflow.array

private[array] class FAZipMapJob[A : ClassManifest,
                                 B : ClassManifest,
                                 C : ClassManifest] private (
  val src: FlatFlowArray[A],
  val osrc: FlowArray[B],
  val dst: FlatFlowArray[C],
  val f: (A,B) => C,
  val offset: Int,
  start: Int,
  end: Int,
  thr: Int,
  obs: FAJob.Observer
) extends FAJob(start, end, thr, obs) {

  protected def subCopy(s: Int, e: Int) = 
    new FAZipMapJob(src, osrc, dst, f, offset, s, e, thresh, this)

  protected def doCompute() {
    osrc.sliceJob(start, end) map { j =>
      delegateThen(Vector(j)) { calculate _ }
    } getOrElse { calculate() }
  }

  private def calculate() {
    for (i <- start to end) {
      dst.data(i + offset) = f(src.data(i), osrc.unsafe(i))
    }
  }

}

object FAZipMapJob {

  def apply[A : ClassManifest, B : ClassManifest, C : ClassManifest](
    src: FlatFlowArray[A],
    osrc: FlowArray[B],
    dst: FlatFlowArray[C],
    f: (A,B) => C,
    offset: Int) =
      new FAZipMapJob(src, osrc, dst, f, offset, 0, src.size - 1, FAJob.threshold(src.size), null)

}
