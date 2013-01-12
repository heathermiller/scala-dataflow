package scala.dataflow.array

import scala.reflect.ClassTag

/**
 * calculate a simple element-to-element transformation on an FA
 */
private[array] class FAMapJob[A : ClassTag, B : ClassTag] private (
  val src: FlatFlowArray[A],
  val dst: FlatFlowArray[B],
  val f: A => B,
  offset: Int,
  start: Int,
  end: Int,
  thr: Int,
  obs: FAJob.Observer
) extends DestFAJob[B](offset, start, end, thr, obs) {

  override protected type SubJob = FAMapJob[A,B]

  override protected def subCopy(s: Int, e: Int) = 
    new FAMapJob(src, dst, f, offset, s, e, thresh, this)

  override protected def doCompute() {
    for (i <- start to end) {
      dst.data(i + offset) = f(src.data(i))
    }
  }

}

private[array] object FAMapJob {

  import FAJob.JobGen

  /** create a new JobGen that creates MapJobs */
  def apply[A : ClassTag, B : ClassTag](
    dst: FlatFlowArray[B],
    f: A => B
  ) = new JobGen[A] {
    def apply(src: FlatFlowArray[A], dstOffset: Int, srcOffset: Int, length: Int) =
      new FAMapJob(src, dst, f, dstOffset - srcOffset, srcOffset,
                   srcOffset + length - 1, FAJob.threshold(length), null)
  }

}
