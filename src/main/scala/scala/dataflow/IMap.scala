package scala.dataflow






trait FlowStream[T] {
}


trait FlowMapLike[K, V, Async[X]] {
  
  def apply(key: K): Async[V]
  
  def get(key: K): Async[Option[V]]
  
  def update(key: K, value: V): Unit
  
  def seal(): FlowMap.Blocking[K, V]
  
  def foreach[U](f: (K, V) => U): Future[Unit]
  
  def onKey[U](key: K)(body: V => U): Unit
  
  def blocking: FlowMap.Blocking[K, V]
  
  def stream: FlowStream[(K, V)]
  
}


trait FlowMap[K, V] extends FlowMapLike[K, V, Future]


trait Future[T] {
  def foreach[U](f: T => U): Unit
  
  def andThen[U](body: =>U): Future[T]
}


object FlowMap extends FlowMapFactory[FlowMapLike] {
  
  def apply[K, V]: FlowMap[K, V] = null
  
}


trait FlowQueueLike[T, Async[X]] {
  
  def head: Async[T]
  
  def tail: Async[FlowQueue[T]]
  
  def isEmpty: Async[Boolean]
  
  def <<(x: T): FlowQueue.Blocking[T]
  
  def foreach[U](f: T => U): Future[Unit]
  
  def seal(): FlowQueue.Blocking[T]
  
  def onBind[U](fs: FlowQueue.Blocking[T] => U): Unit
  
  def blocking: FlowQueue.Blocking[T]
  
  def stream: FlowStream[T]
  
}


trait FlowQueue[T] extends FlowQueueLike[T, Future]


object << {
  
  def unapply[T](fs: FlowQueue.Blocking[T]): Option[(T, FlowQueue[T])] =
    if (fs.isEmpty) None
    else Some((fs.head, fs.tail))
  
}


object Seal {
  
  def unapply[T](fs: FlowQueue.Blocking[T]): Boolean = fs.isEmpty
  
}


trait FlowFactory[Flow[X, Z[_]]] {
  
  type Blocking[T] = Flow[T, Id]
  
}


trait FlowMapFactory[Flow[X, Y, Z[_]]] {
  
  type Blocking[K, V] = Flow[K, V, Id]
  
}


object FlowQueue extends FlowFactory[FlowQueueLike] {
  
  def apply[T](): FlowQueue[T] = null
  
}


trait FlowVarLike[T, Async[X]] {
  
  def <<(x: T): Unit
  
  def apply(): Async[T]
  
  def blocking: FlowVar.Blocking[T]
  
  def stream: FlowStream[T]
  
}


trait FlowVar[T] extends FlowVarLike[T, Future]


object FlowVar extends FlowFactory[FlowVarLike] {
  
  def apply[T](): FlowVar[T] = null
  
}







