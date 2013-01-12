package scala.dataflow.array

import scala.dataflow.Future
import scala.reflect.ClassTag

/**
 * out-of-order folding job on a FA. Optionally supports a map before
 * reduction.
 *
 * Result combination after completion is done by superclass.
 */
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

  override protected def subCopy(s: Int, e: Int) = 
    new FAFoldJob(src, z, f, g, s, e, thresh, this)

  override protected def doCompute() {
    var tmp = z
    for (i <- start to end) {
      tmp = f(tmp, g(src.data(i)))
    }
    setResult(tmp)
  }

  override protected def combineResults(x: A1, y: A1) = f(x,y)

}

private[array] object FAFoldJob {

  import FAJob.JobGen

  /** create a new FAFoldJob with map-phase */
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

  /** create a new FAFoldJob without map-phase */
  def apply[A : ClassTag, A1 >: A](
    src: FlatFlowArray[A],
    srcOffset: Int,
    length: Int,
    z: A1,
    f: (A1, A1) => A1
  ): FAFoldJob[A,A1] = FAFoldJob(src, srcOffset, length, z, f, (x: A) => x)

}
