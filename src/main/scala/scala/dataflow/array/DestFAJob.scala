package scala.dataflow.array

import scala.reflect.ClassTag

private[array] abstract class DestFAJob[A : ClassTag](
  val offset: Int,
  start:  Int,
  end:    Int,
  thresh: Int,
  observer: FAJob.Observer
) extends FAJob(start, end, thresh, observer) {
  
  // Protected since currently unused
  protected def dst: FlowArray[A]

  override protected def covers(from: Int, to: Int) =
    super.covers(from - offset, to - offset)

}

