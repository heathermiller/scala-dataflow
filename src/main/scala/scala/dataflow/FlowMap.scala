package scala.dataflow






trait FlowMapLike[K, V, Async[X]] extends FlowLike[(K, V)] {
  
  def apply(key: K): Async[V]
  
  /** Gets the value associated with the key.
   *  The value is available only once the key is added to the flow map,
   *  or the flow map is sealed, in which case `get` returns a `None`.
   *  
   *  {{{
   *  val v1: Future[Option[Int]] = fm.get(1)
   *  val v2: Option[Int] = fm.blocking.get(1)
   *  }}}
   */
  def get(key: K): Async[Option[V]]
  
  def update(key: K, value: V): Unit
  
  def seal(): FlowMap.Blocking[K, V]
  
  /** A foreach loop on all the elements.
   *  Completes when the map is sealed.
   *  
   *  {{{
   *  (for (kv <- fm) {
   *    process(kv)
   *  }) andThen {
   *    finish()
   *  }
   *  }}}
   */
  def foreach[U](f: (K, V) => U): Async[Unit]
  
  def onKey[U](key: K)(body: V => U): Unit
  
  def blocking: FlowMap.Blocking[K, V]
  
}


trait FlowMap[K, V] extends FlowMapLike[K, V, Future]


object FlowMap extends FlowMapFactory[FlowMapLike] {
  
  def apply[K, V]: FlowMap[K, V] = null
  
}







