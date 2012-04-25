package scala.dataflow.impl



import sun.misc.Unsafe



final class FlowHashTable[K, V] {
  
  @volatile
  private var data = newTable(initialSlots)
  private val unsafe = getUnsafe()
  private val ARRAYOFFSET = unsafe.arrayBaseOffset(classOf[Array[AnyRef]])
  private val ARRAYSTEP = unsafe.arrayIndexScale(classOf[Array[AnyRef]])
  
  final class State {
    
  }
  
  private def index(k: K, len: Int): Int = {
    (k.## & ((len >> 1) - 1)) << 1
  }
  
  private def sizeForSlots(sz: Int) = (sz << 1)
  
  private def initialSlots = 2 << 22
  
  private def newTable(slots: Int) = {
    val table = new Array[AnyRef](sizeForSlots(slots))
    table(0) = new State
    table
  }
  
  private def RAWPOS(idx: Int) = ARRAYOFFSET + idx * ARRAYSTEP
  
  private def SAKEY(data: Array[AnyRef], idx: Int, key: K) = {
    unsafe.compareAndSwapObject(data, RAWPOS(idx), null, key)
  }
  
  private def SAVALUE(data: Array[AnyRef], idx: Int, value: V) = {
    unsafe.compareAndSwapObject(data, RAWPOS(idx + 1), null, value)
  }
  
  def update(k: K, v: V) {
    val data = this.data
    val len = data.length
    var idx = index(k, len)
    
    while (true) {
      while (data(idx) eq null) {
        if (SAKEY(data, idx, k)) {
          while (data(idx + 1) eq null) {
            if (SAVALUE(data, idx, v)) return
          }
        }
      }
      
      idx = (idx + 2) & (len - 1)
    }
  }
  
  override def toString = "[%s]"format(data.mkString(", "))
  
}
