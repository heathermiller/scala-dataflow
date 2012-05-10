package scala.dataflow



import annotation.tailrec



object FlowPoolBench extends ParInsertBench {
  import Utils._
  
  val data = new Data(0)
  var pool = new impl.FlowPool[Data]()
  
  class Inserter(val sz: Int) extends Thread {
    override def run() {
      val build = pool.builder
      var i = 0
      while (i < sz) {
        build << data
        i += 1
      }
    }
  }
  
  def inserter(sz: Int) = new Inserter(sz)
  
  override def setUp() {
    pool = new impl.FlowPool[Data]()
  }
  
}


object FlowPoolBench2 extends testing.Benchmark with Utils.Props {
  import Utils._
  
  override def run() {
    val work = size
    //val pool = new impl.FlowPool[Data]()
    val builder = new impl.FlowPool.Builder[Data]()
    //val builder = pool.builder
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
      val pool = new ExpPool[Data](work)
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


final class ExpPool[T](private val blocksize: Int) {
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
  
  def advance() {
    var pos = lastpos
    while (!array(pos).isInstanceOf[List[_]]) pos += 1
    lastpos = pos
  }
  
  @tailrec
  def <<(elem: T): this.type = if (lastpos < blocksize) {
    val pos = lastpos
    val npos = pos + 1
    val next = array(npos)
    val curr = array(pos)
    if (curr.isInstanceOf[List[_]]) {
      if (CAS(npos, next, curr)) {
        if (CAS(pos, curr, elem.asInstanceOf[AnyRef])) {
          lastpos = npos
          this
        } else <<(elem)
      } else <<(elem)
    } else {
      advance()
      <<(elem)
    }
  } else throw new Exception
  
}





