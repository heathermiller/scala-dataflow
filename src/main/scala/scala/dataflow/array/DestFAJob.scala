package scala.dataflow.array

import scala.reflect.ClassTag

/**
 * abstract superclass for all FAJobs that write to a
 * FlowArray as destination.
 *
 * Mainly used to have a common implementation of covers
 */
private[array] abstract class DestFAJob[A : ClassTag](
  /**
   * offset between source and target index
   * tidx = sidx + offset
   */
  val offset: Int,
  start:  Int,
  end:    Int,
  thresh: Int,
  observer: FAJob.Observer
) extends FAJob(start, end, thresh, observer) {
  
  /**
   * the FlowArray this FAJob writes to
   * Protected since currently unused by any other class
   */
  protected def dst: FlowArray[A]

  override protected def covers(from: Int, to: Int) =
    super.covers(from - offset, to - offset)

}

