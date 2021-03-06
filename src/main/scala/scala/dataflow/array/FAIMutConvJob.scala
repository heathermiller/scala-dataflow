package scala.dataflow.array

import scala.reflect.ClassTag

/**
 * Immutable convergance job. Iterates over a function until a given
 * condition is met. A special case of a map.
 */
private[array] class FAIMutConvJob[A : ClassTag] private (
  val src: FlatFlowArray[A],
  val dst: FlatFlowArray[A],
  val f: A => A,
  val cond: A => Boolean,
  offset: Int,
  start: Int,
  end: Int,
  thr: Int,
  obs: FAJob.Observer
) extends DestFAJob[A](offset, start, end, thr, obs) {

  override protected type SubJob = FAIMutConvJob[A]

  override protected def subCopy(s: Int, e: Int) = 
    new FAIMutConvJob(src, dst, f, cond, offset, s, e, thresh, this)

  override protected def doCompute() {
    for (i <- start to end) {
      var x = src.data(i)
      while (!cond(x)) {
        x = f(x)
      }
      dst.data(i + offset) = x
    }
  }

}

private[array] object FAIMutConvJob {

  import FAJob.JobGen

  /** create an new JobGen that creates FAIMutConvJobs */
  def apply[A : ClassTag](
    dst: FlatFlowArray[A],
    f: A => A,
    cond: A => Boolean
  ) = new JobGen[A] {
    def apply(src: FlatFlowArray[A], dstOffset: Int, srcOffset: Int, length: Int) =
      new FAIMutConvJob(src, dst, f, cond, dstOffset - srcOffset, srcOffset,
                   srcOffset + length - 1, FAJob.threshold(length), null)
  }

}
