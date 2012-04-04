package scala






package object dataflow {
  
  type Id[X] = X
  
  def task[U](body: =>U) {
  }
  
  def select[T](flows: FlowLike[T]*): FlowReader[T] = {
    null
  }
  
  case class WithdrawCallbackException() extends util.control.ControlThrowable
  
  def withdraw: Nothing = throw new WithdrawCallbackException
  
}


package dataflow {
  
  trait FlowLike[T] {
    
    def reader: FlowReader[T]
    
    def onBind[U](body: T => U): Unit
    
  }
  
  trait FlowMapFactory[Flow[X, Y, Z[_]]] {
    
    type Blocking[K, V] = Flow[K, V, Id]
    
  }
  
  trait FlowFactory[Flow[X, Z[_]]] {
    
    type Blocking[T] = Flow[T, Id]
    
  }
  
  trait Future[T] {
    def foreach[U](f: T => U): Unit
    
    def andThen[U](body: =>U): Future[T]
  }
}
