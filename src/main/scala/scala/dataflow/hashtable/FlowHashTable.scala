package scala.dataflow
package hashtable



import sun.misc.Unsafe



final class FlowHashTable[K, V] {
  
  @volatile
  private var table = newTable(initialSlots)
  private val unsafe = getUnsafe()
  private val ARRAYOFFSET = unsafe.arrayBaseOffset(classOf[Array[AnyRef]])
  private val ARRAYSTEP = unsafe.arrayIndexScale(classOf[Array[AnyRef]])
  
  /* hash table states */
  
  object Open
  
  // indicates that an array is being allocated for the new size by at least some processor
  final class StartingResize(val starts: Seq[Long])
  
  // indicates that an array has been allocated for the new size and copying has commenced
  final class Copying(val ntable: Array[AnyRef]) {
    // indicates which blocks have been copied already
    val copiedblock = new Array[Boolean](Runtime.getRuntime.availableProcessors)
  }
  
  /* private methods */
  
  private def index(k: K, len: Int): Int = {
    (k.## & ((len >> 1) - 1)) << 1
  }
  
  private def sizeForSlots(sz: Int) = (sz << 1)
  
  private def initialSlots = 2 << 22
  
  private def newTable(slots: Int) = {
    val ntable = new Array[AnyRef](sizeForSlots(slots))
    ntable(0) = Open
    ntable
  }
  
  private def RAWPOS(idx: Int) = ARRAYOFFSET + idx * ARRAYSTEP
  
  private def SA_KEY(table: Array[AnyRef], idx: Int, key: K) = {
    unsafe.compareAndSwapObject(table, RAWPOS(idx), null, key)
  }
  
  private def SA_VALUE(table: Array[AnyRef], idx: Int, value: V) = {
    unsafe.compareAndSwapObject(table, RAWPOS(idx + 1), null, value)
  }
  
  private def CAS_STATE(table: Array[AnyRef], oldstate: AnyRef, newstate: AnyRef) = {
    unsafe.compareAndSwapObject(table, RAWPOS(0), oldstate, newstate)
  }
  
  private def READ_STATE(table: Array[AnyRef]) = table(0)
  
  private def retry_limit(len: Int) = 10 + (len >> 3)
  
  private def helpResizeOnLookup(state: AnyRef, criticalIndex: Int) {
    // if you've noticed a key in the old table and the new array is being allocated
    // then the key will surely be copied - stop search
    // TODO
    
    // however, if the new array is already allocated
    // then the key may not be copied to the new array before you return it
    // in this case - try to copy the key yourself before returning it
    // TODO
  }
  
  private def helpResizeOnUpdate(state: AnyRef, criticalIndex: Int) {
    // see if sleep is needed before the allocation starts
    // in case that some processor is already allocating the array
    // we want to avoid creating too much garbage, so we wait a bit
    // TODO
    
    // allocate if necessary
    // TODO
    
    // help with the resize if necessary
    // - copy any block of the array that is not already copied
    // - assign the new array to the hash table if it is not already assigned
    // TODO
    
    // copy the element at the critical index
    // TODO
  }
  
  /* public api */
  
  def update(k: K, v: V) {
    val table = /*READ*/this.table
    val len = table.length
    var idx = index(k, len)
    var probes = 0
    
    // if table is in the open state, we can try to update it
    if (READ_STATE(table) == Open) while (true) {
      // we search through the table until we find an empty slot for the key
      // (or a slot with the equal key, but no value assigned yet)
      // we have to account for spurious CAS failures here, hence the loop
      var keyAtIdx = table(idx)
      var valueAtIdx = table(idx + 1)
      var emptyKey = keyAtIdx eq null
      var sameKey = !emptyKey && keyAtIdx == k
      while (emptyKey || (sameKey && (valueAtIdx eq null))) {
        // we single-assign the key - CAS with the expected value being null
        if (!emptyKey || SA_KEY(table, idx, k)) {
          // if successful, we single-assign the corresponding value (CAS expecting null)
          // (it is also possible that some other thread assigned the value after the key)
          while (table(idx + 1) eq null) {
            if (SA_VALUE(table, idx, v)) {
              // if the value is successfully assigned
              val state = READ_STATE(table)
              if (state == Open) return
              else helpResizeOnUpdate(state, idx)
            }
          }
        }
        // update last read key and value at idx
        keyAtIdx = table(idx)
        valueAtIdx = table(idx + 1)
        // if previously empty, but now not empty, compare them
        if (emptyKey && (keyAtIdx ne null)) {
          emptyKey = false
          sameKey = keyAtIdx == k
        }
      }
      if (sameKey) throw new UnsupportedOperationException("Key %s already assigned to %s".format(k, v))
      
      // next slot
      idx = (idx + 2) & (len - 1)
    }
  }
  
  override def toString = "[%s]".format(table.mkString(", "))
  
}
