package scala.dataflow.array

import scala.reflect.ClassTag

private[array] class FAFlatMapJob[A : ClassTag, B : ClassTag] private (
  val src: FlatFlowArray[A],
  val dst: HierFlowArray[B],
  val f: A => FlowArray[B],
  val n: Int,
  of: Int,
  start: Int,
  end: Int,
  thr: Int,
  obs: FAJob.Observer
) extends DestFAJob[B](of, start, end, thr, obs) {

  override protected type SubJob = FAFlatMapJob[A,B]

  protected def subCopy(s: Int, e: Int) = 
    new FAFlatMapJob(src, dst, f, n, offset, s, e, thresh, this)

  protected def doCompute() {
    for (i <- start to end) {
      val sub = f(src.data(i))
      assert(n == sub.size)
      dst.subData(i + offset) = sub
    }
  }

  override protected def covers(from: Int, to: Int) = {
    val is = dst.subSize
    (from - offset) / is >= start &&
    (to   - offset) / is <= end
  }

}

object FAFlatMapJob {

  import FAJob.JobGen

  def apply[A : ClassTag, B : ClassTag](
    dst: HierFlowArray[B],
    f: A => FlowArray[B],
    n: Int
  ) = new JobGen[A] {
    def apply(src: FlatFlowArray[A], dstOffset: Int, srcOffset: Int, length: Int) =
      new FAFlatMapJob(src, dst, f, n, dstOffset, srcOffset,
                       srcOffset + length - 1, FAJob.threshold(length), null)
    // might this be a better choice (or something the like?)
    // FAJob.threshold(length * dst.subSize) / dst.subSize, null)
  }

}
