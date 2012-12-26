package scala.dataflow.array

import scala.dataflow.Future
import scala.reflect.ClassTag

private[array] class FAFoldJob[A : ClassTag, A1] private (
  val src: FlatFlowArray[A],
  val z: A1,
  val f: (A1, A1) => A1,
  val g: A => A1,
  start: Int,
  end: Int,
  thr: Int,
  obs: FAJob.Observer
) extends FAResultJob[A1](start, end, thr, obs) {

  override protected type SubJob = FAFoldJob[A,A1]

  protected def subCopy(s: Int, e: Int) = 
    new FAFoldJob(src, z, f, g, s, e, thresh, this)

  protected def doCompute() {
    var tmp = z
    for (i <- start to end) {
      tmp = f(tmp, g(src.data(i)))
    }
    setResult(tmp)
  }

  protected override def combineResults(x: A1, y: A1) = f(x,y)

}

object FAFoldJob {

  import FAJob.JobGen

  def apply[A : ClassTag, A1](
    src: FlatFlowArray[A],
    srcOffset: Int,
    length: Int,
    z: A1,
    f: (A1, A1) => A1,
    g: A => A1
  ): FAFoldJob[A,A1] =
    new FAFoldJob(src, z, f, g, srcOffset,
                  srcOffset + length - 1, FAJob.threshold(length), null)

  def apply[A : ClassTag, A1 >: A](
    src: FlatFlowArray[A],
    srcOffset: Int,
    length: Int,
    z: A1,
    f: (A1, A1) => A1
  ): FAFoldJob[A,A1] = FAFoldJob(src, srcOffset, length, z, f, (x: A) => x)

}
