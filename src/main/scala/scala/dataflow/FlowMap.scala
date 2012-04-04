package scala.dataflow






trait FlowMapLike[K, V, Async[X]] extends FlowLike[(K, V)] {
  
  def apply(key: K): Async[V]
  
  def get(key: K): Async[Option[V]]
  
  /**
  *  Same semantics as normal `update` on a map, except for after `seal` is called on this map. In this case, some kind of exception is thrown.
  */
  def update(key: K, value: V): Unit
  
  def seal(): FlowMap.Blocking[K, V]
  
  def foreach[U](f: (K, V) => U): Async[Unit]
  
  def onKey[U](key: K)(body: V => U): Unit
  
  def blocking: FlowMap.Blocking[K, V]
  
}


trait FlowMap[K, V] extends FlowMapLike[K, V, Future]


object FlowMap extends FlowMapFactory[FlowMapLike] {
  
  def apply[K, V]: FlowMap[K, V] = null
  
}







