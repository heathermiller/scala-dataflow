package scala.dataflow.array

import scala.dataflow.Future
import scala.annotation.tailrec

class FlowArraySliceView[A : ClassManifest](
  private val data: FlowArray[A],
  private val offset: Int,
  val size: Int
) extends FlowArray[A] {

  // Cache job which is responsible for this view. Values:
  // None: not yet populated
  // Some: jobs to be scanned
  // null: done
  @volatile private var sliceJobCache: Option[IndexedSeq[FAJob]] = None 

  @inline private def getSlice = {

    def fetch = {
      val res = data.sliceJobs(offset, offset + size - 1)

      res.map {
        case (js, false) => sliceJobCache = Some(js)
        case _ => 
      } getOrElse { sliceJobCache = null }
      
      res
    }

    sliceJobCache match {
      case Some(js) =>
        val filter = js.filterNot(_.done)
        if (filter.isEmpty) {
          sliceJobCache = null
          None
        } else {
          sliceJobCache = Some(filter)
          Some((filter, false))
        }
      case None => fetch
      case null => None
    }
  }

  private[array] def copyToArray(dst: Array[A], srcPos: Int, dstPos: Int, length: Int) {
    data.copyToArray(dst, srcPos + offset, dstPos, length)
  }

  private[array] def sliceJobs(from: Int, to: Int) =
    data.sliceJobs(offset + from, offset + to)

  private[array] def dispatch(gen: JobGen, dstOffset: Int, srcOffset: Int, length: Int) =
    null // TODO

  private[array] def tryAddObserver(obs: FAJob.Observer) =
    // TODO this notifies too late (but works)!
    data.tryAddObserver(obs)

  /** Checks if this job is done */
  def done = getSlice map {
    case (_, true) => false
    case (j, false) => j.forall(_.done)
  } getOrElse true

  def unsafe(i: Int) = data.unsafe(i + offset)
  def blocking(isAbs: Boolean, msecs: Long) = {
    block(isAbs, msecs)

    val ret = new Array[A](size)
    copyToArray(ret, offset, 0, size)
    ret
  }

}
