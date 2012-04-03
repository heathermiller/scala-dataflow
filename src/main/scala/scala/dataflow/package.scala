package scala






package object dataflow {
  
  type Id[X] = X
  
  def task[U](body: =>U) {
  }
  
}


