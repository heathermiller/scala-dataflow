package scala.dataflow.array

import scala.dataflow.Future
import scala.annotation.tailrec

class FlatFlowArray[A : ClassManifest](
  private[array] val data: Array[A]
) extends FlowArray[A] with FAJob.Observer {

  import FlowArray._

  // Fields
  val size = data.length

  final private[array] def dispatch(gen: JobGen, offset: Int) = {
    val job = gen(this, offset)
    dispatch(job)
    job
  }

  final private[array] def copyToArray(trg: Array[A], offset: Int) {
    Array.copy(data, 0, trg, offset, size)
  }

  def fold[A1 >: A](z: A1)(op: (A1, A1) => A1): Future[A1] = {
    import FAFoldJob.FoldFuture

    val job = FAFoldJob(this, z, op)
    val fut = new FoldFuture(job)
    job.addObserver(fut)

    dispatch(job)
    fut
  }

  override def blocking = {
    block()
    data
  }

}
