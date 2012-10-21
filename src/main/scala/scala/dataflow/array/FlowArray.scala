package scala.dataflow.array

import scala.annotation.tailrec

class FlowArray[A : ClassManifest] private (
  private val data: Array[A],
  full: Boolean) {

  import FlowArray._

  // Public constructor
  def this(data: Array[A]) = this(data, true)

  // Information about blocks
  private val blCount = 8  
  private val size = data.length
  private val blSize = math.ceil(size.toDouble / blCount).toInt
  @volatile private var doneCount: Int = if (full) blCount else 0

  private val blStates: Array[BlockState] = {
    if (full)
      Array.fill(blCount)(Done)
    else 
      Array.fill(blCount)(Waiting)
  }

  // Utility
  private val unsafe = getUnsafe()
  private val ARRAYOFFSET      = unsafe.arrayBaseOffset(classOf[Array[BlockState]])
  private val ARRAYSTEP        = unsafe.arrayIndexScale(classOf[Array[BlockState]])
  private val DCOFFSET         = unsafe.objectFieldOffset(classOf[FlowArray[_]].getDeclaredField("doneCount"))
  @inline private def RAWPOS(idx: Int) = ARRAYOFFSET + idx * ARRAYSTEP
  @inline private def CAS(bl: Array[BlockState], idx: Int, ov: BlockState, nv: BlockState) =
    unsafe.compareAndSwapObject(bl, RAWPOS(idx), ov, nv)
  @inline private def CAS_DONECOUNT(ov: Int, nv: Int) =
    unsafe.compareAndSwapInt(this, DCOFFSET, ov, nv)

  // Blocking management
  private def doneBlock(bli: Int) = {

    @tailrec
    def inc: Int = {
      val ov = /*READ*/doneCount
      if (!CAS_DONECOUNT(ov, ov + 1)) inc
      else ov + 1
    }

    blStates(bli) = Done /*WRITE*/

    val cc = inc
    
    if (cc == blCount) {
      // TODO any nicer way to do this?
      synchronized { this.notifyAll() }
    }

  }

  // Functions
  def map[B : ClassManifest](f: A => B) = {
    val ret = new FlowArray(new Array[B](data.length), false)

    for (bli <- 0 to blCount - 1) {
      val work = mapJob(bli, this, ret, f)

      val njob = blStates(bli) /* READ */ match {
        case Waiting =>
          throw new IllegalStateException("Unassigned block in dependant FlowArray")
        case Done =>
          FlowArrayJob(work)
        case Assigned(job) =>
          if (!job.add(work))
            FlowArrayJob(work)
          else
            job
      }

      CAS(ret.blStates, bli, Waiting, Assigned(njob))

    }

    ret
  }

  def done = /*READ*/doneCount == blCount

  def blocking = {
    synchronized {
      while (!done) wait()
    }
    data
  }
  

}

object FlowArray {

  sealed abstract class BlockState
  case object Done extends BlockState
  case class  Assigned(job: FlowArrayJob) extends BlockState
  case object Waiting extends BlockState

  def mapJob[A : ClassManifest,B : ClassManifest](
    bli: Int,
    src: FlowArray[A],
    dest: FlowArray[B],
    f: A => B
  ) = () => {

    val offset = bli * src.blSize
    for (i <- offset to math.min(offset + src.blSize, src.size) - 1) {
      dest.data(i) = f(src.data(i))
    }

    dest.doneBlock(bli)

  }

}
