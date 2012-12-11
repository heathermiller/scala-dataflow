package scala.dataflow.array

import scala.dataflow.Future

private[array] class FATransposeJob[A : ClassManifest] private (
  val src: FlatFlowArray[A],
  val dst: FlatFlowArray[A],
  val step: Int,
  val srcOffset: Int,
  val dstOffset: Int,
  start: Int,
  end: Int,
  thr: Int,
  obs: FAJob.Observer
) extends FAJob(start, end, thr, obs) {

  override protected type SubJob = FATransposeJob[A]

  protected def subCopy(s: Int, e: Int) = 
    new FATransposeJob(src, dst, step, srcOffset, dstOffset, s, e, thresh, this)

  protected def doCompute() {
    for (i <- start to end) {
      val oind = i - srcOffset
      val nind = (oind / step) + (oind % step) * step
      dst.data(nind + dstOffset) = src.data(i)
    }
  }

  /** this thing does not really cover any range */
  protected override def covers(from: Int, to: Int) = false

}

object FATransposeJob {

  import FAJob.JobGen

  def apply[A : ClassManifest](
    dst: FlatFlowArray[A],
    step: Int
  ) = new JobGen[A] {
    def apply(src: FlatFlowArray[A], dstOffset: Int, srcOffset: Int, length: Int) =
      // TODO have a better size threshold
      new FATransposeJob(src, dst, step, srcOffset, dstOffset, srcOffset,
                         srcOffset + length - 1, FAJob.threshold(length), null)
  }

}
