package scala.dataflow.array

import scala.reflect.ClassTag

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

  protected def subCopy(s: Int, e: Int) = 
    new FAIMutConvJob(src, dst, f, cond, offset, s, e, thresh, this)

  protected def doCompute() {
    for (i <- start to end) {
      var x = src.data(i)
      while (!cond(x)) {
        x = f(x)
      }
      dst.data(i + offset) = x
    }
  }

}

object FAIMutConvJob {

  import FAJob.JobGen

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
