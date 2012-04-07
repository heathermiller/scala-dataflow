package scala.dataflow






trait FlowMapLike[K, V, Async[X]] extends FlowLike[(K, V)] {
  
  def apply(key: K)(implicit a: FlowMap.Apply[Async]): Async[V]
  
  /** Gets the value associated with the key.
   *  The value is available only once the key is added to the flow map,
   *  or the flow map is sealed, in which case `get` returns a `None`.
   *  
   *  {{{
   *  val v1: Future[Option[Int]] = fm.get(1)
   *  val v2: Option[Int] = fm.blocking.get(1)
   *  }}}
   */
  def get(key: K)(implicit a: FlowMap.Get[Async]): Async[Option[V]]
  
  def <<(kv: (K, V)): this.type
  
  /** Same semantics as normal `update` on a map, except for after `seal` is called on this map. In this case, some kind of exception is thrown.
   */
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
  def foreach[U](f: (K, V) => U)(implicit a: FlowMap.Foreach[Async]): Async[Unit]
  
  def onKey[U](key: K)(body: V => U): Unit
  
  def blocking: FlowMap.Blocking[K, V]
  
}


trait FlowMap[K, V] extends FlowMapLike[K, V, Future]


object FlowMap extends FlowMapFactory[FlowMapLike] {
  
  def apply[K, V]: FlowMap[K, V] = null
  
  trait Apply[Async[X]] {
    def apply[K, V](coll: FlowMapLike[K, V, Async], key: K): Async[V]
  }
  
  implicit object FutureApply extends Apply[Future] {
    def apply[K, V](coll: FlowMapLike[K, V, Future], key: K): Future[V] = null
  }
  
  implicit object IdApply extends Apply[Id] {
    def apply[K, V](coll: FlowMapLike[K, V, Id], key: K): V = throw new Exception
  }
  
  trait Get[Async[X]] {
    def apply[K, V](coll: FlowMapLike[K, V, Async], key: K): Async[Option[V]]
  }
  
  implicit object FutureGet extends Get[Future] {
    def apply[K, V](coll: FlowMapLike[K, V, Future], key: K): Future[Option[V]] = null
  }
  
  implicit object IdGet extends Get[Id] {
    def apply[K, V](coll: FlowMapLike[K, V, Id], key: K): Option[V] = throw new Exception
  }
  
  trait Foreach[Async[X]] {
    def apply[K, V, U](coll: FlowMapLike[K, V, Async], f: (K, V) => U): Async[Boolean]
  }
  
  implicit object FutureForeach extends Foreach[Future] {
    def apply[K, V, U](coll: FlowMapLike[K, V, Future], f: (K, V) => U) = null
  }
  
  implicit object IdForeach extends Foreach[Id] {
    def apply[K, V, U](coll: FlowMapLike[K, V, Id], f: (K, V) => U) = throw new Exception
  }
  
}







