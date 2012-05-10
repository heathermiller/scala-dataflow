package scala.dataflow



import annotation.tailrec



// object FlowPoolBench extends ParInsertBench {
//   import Utils._
  
//   val data = new Data(0)
//   var pool = new impl.FlowPool[Data]()
  
//   class Inserter(val sz: Int) extends Thread {
//     override def run() {
//       val build = pool.builder
//       var i = 0
//       while (i < sz) {
//         build << data
//         i += 1
//       }
//     }
//   }
  
//   def inserter(sz: Int) = new Inserter(sz)
  
//   override def setUp() {
//     pool = new impl.FlowPool[Data]()
//   }
  
// }


object FlowPoolBench2 extends testing.Benchmark with Utils.Props {
  import Utils._
  
  override def run() {
    val pool = new impl.FlowPool[Data]()
    //val builder = pool.builder
    val builder = new impl.Builder[Data](pool.initBlock)
    
    test(builder)
  }
  
  private def test(builder: impl.Builder[Data]) {
    val work = size
    val data = new Data(0)
    var i = 0
    
    while (i < work) {
      builder << data
      i += 1
    }
  }
  
}


object FlowPoolExperiment extends ParInsertBench {
  import Utils._
  
  class Inserter(val sz: Int) extends Thread {
    override def run() {
      val work = size
      val pool = new FlowPoolBuilder[Data](work)
      val data = new Data(0)
      var i = 0
      
      while (i < work) {
        pool << data
        i += 1
      }
    }
  }
  
  def inserter(sz: Int) = new Inserter(sz)
  
}


final class FlowPoolBuilder[T](private val blocksize: Int) {
  private val unsafe = impl.getUnsafe()
  private val ARRAYOFFSET = unsafe.arrayBaseOffset(classOf[Array[AnyRef]])
  private val ARRAYSTEP = unsafe.arrayIndexScale(classOf[Array[AnyRef]])
  @volatile private var array = new Array[AnyRef](blocksize + 4)
  @volatile private var lastpos = 0
  
  array(0) = Nil
  
  def RAWPOS(idx: Int) = ARRAYOFFSET + ARRAYSTEP * idx
  
  def CAS(idx: Int, ov: AnyRef, nv: AnyRef): Boolean = {
    unsafe.compareAndSwapObject(array, RAWPOS(idx), ov, nv)
  }
  
  @tailrec
  def <<(elem: T): this.type = {
    val pos = lastpos
    val npos = pos + 1
    val next = array(npos)
    val curr = array(pos)
    if (curr.isInstanceOf[List[_]] && (next ne End)) {
      if (CAS(npos, next, curr)) {
        if (CAS(pos, curr, elem.asInstanceOf[AnyRef])) {
          lastpos = npos
          this
        } else <<(elem)
      } else <<(elem)
    } else slowadd(elem)
  }
  
  def slowadd(elem: T): this.type = {
    advance()
    <<(elem)
  }
  
  @tailrec
  private def advance() {
    val pos = lastpos
    val obj = array(pos)
    if (obj eq Seal) sys.error("Insert on sealed structure")
    if (!obj.isInstanceOf[List[_]]) {
      lastpos = pos + 1
      advance()
    } else if (pos >= blocksize) {
      val ob = array(blocksize + 1).asInstanceOf[Array[AnyRef]]
      if (ob eq null) {
        val nb = new Array[AnyRef](blocksize + 2)
        nb(0) = array(blocksize)
        CAS(blocksize + 1, ob, nb)
      }
      // TODO we have a race here
      array = array(blocksize + 1).asInstanceOf[Array[AnyRef]]
      lastpos = 0
    }
  }
  
}


object End


