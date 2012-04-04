package scala.dataflow






trait FlowMapLike[K, V, Async[X]] extends FlowLike[(K, V)] {
  
  def apply(key: K): Async[V]
  
  def get(key: K): Async[Option[V]]
  
  def update(key: K, value: V): Unit
  
  def seal(): FlowMap.Blocking[K, V]
  
  def foreach[U](f: (K, V) => U): Future[Unit]
  
  def onKey[U](key: K)(body: V => U): Unit
  
  def blocking: FlowMap.Blocking[K, V]
  
}


trait FlowMap[K, V] extends FlowMapLike[K, V, Future]


object FlowMap extends FlowMapFactory[FlowMapLike] {
  
  def apply[K, V]: FlowMap[K, V] = null
  
}







