package scala.dataflow.array

import scala.reflect.ClassTag

private[array] class FAMutConvJob[A : ClassTag, B, C : ClassTag] private (
  val src: FlatFlowArray[A],
  val dst: FlatFlowArray[C],
  val toMut: A => B,
  val toIMut: B => C,
  val f: B => Unit,
  val cond: B => Boolean,
  offset: Int,
  start: Int,
  end: Int,
  thr: Int,
  obs: FAJob.Observer
) extends DestFAJob[C](offset, start, end, thr, obs) {

  override protected type SubJob = FAMutConvJob[A,B,C]

  protected def subCopy(s: Int, e: Int) = 
    new FAMutConvJob(src, dst, toMut, toIMut, f, cond, offset, s, e, thresh, this)

  protected def doCompute() {
    for (i <- start to end) {
      var x = toMut(src.data(i))
      while (!cond(x)) { f(x) }
      dst.data(i + offset) = toIMut(x)
    }
  }

}

private[array] object FAMutConvJob {

  import FAJob.JobGen

  def apply[A : ClassTag, B, C : ClassTag](
    dst: FlatFlowArray[C],
    toMut: A => B,
    toIMut: B => C,
    f: B => Unit,
    cond: B => Boolean
  ) = new JobGen[A] {
    def apply(src: FlatFlowArray[A], dstOffset: Int, srcOffset: Int, length: Int) =
      new FAMutConvJob(src, dst, toMut, toIMut, f, cond,
                       dstOffset - srcOffset, srcOffset,
                       srcOffset + length - 1, FAJob.threshold(length), null)
  }

}
