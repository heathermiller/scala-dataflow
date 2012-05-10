package scala.dataflow.scheduler






trait Coroutine {
  
  def run(): Unit
  
  def isReady: Boolean
  
  def isDone: Boolean
  
}


