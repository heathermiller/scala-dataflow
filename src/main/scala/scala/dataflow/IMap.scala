package scala.dataflow






trait IMap[K, V] {
  
  def value(key: K): V
  
  def apply(key: K): Future[V]
  
  def update(key: K, value: V): Unit
  
  def seal(): IMap[K, V]
  
  def foreach[U](f: (K, V) => U): Unit
  
  def onKey[U](key: K)(body: V => U): Unit
  
}


trait Future[T] {
  def foreach[U](f: T => U)
}


object IMap {
  
  def apply[K, V]: IMap[K, V] = null
  
  def dynamicProgrammingBlocking() {
    val dictionary = Set("go", "got", "here", "there")
    val longest = dictionary.maxBy(_.length).length
    val text: String = "gothere"
    val solutions = IMap[String, Seq[List[String]]]
    solutions("") = Nil
    
    def interpret(suffix: String) {
      val possibilities = for {
        length <- 0 until (longest min suffix.length)
        val firstword = suffix.substring(0, length)
        if dictionary(firstword)
        solution <- solutions.value(suffix.substring(length, suffix.length))
      } yield firstword :: solution
      
      solutions(suffix) = possibilities
    }
    
    for (i <- (text.length - 1) to 0 by -1) yield task {
      interpret(text.substring(i, text.length))
    }
    
    solutions(text)
  }
  
  def dynamicProgrammingMonadic() {
    val dictionary = Set("go", "got", "here", "there")
    val longest = dictionary.maxBy(_.length).length
    val text: String = "gothere"
    val solutions = IMap[String, Seq[List[String]]]
    solutions("") = Nil
    
    def interpret(suffix: String) {
      for (
        length <- 0 until (longest min suffix.length);
        val firstword = suffix.substring(0, length);
        if dictionary(firstword);
        possibleSolutions <- solutions(suffix.substring(length, suffix.length))
      ) {
        val extendedSolutions = for (solution <- possibleSolutions) yield firstword :: solution
        solutions(suffix) = extendedSolutions
      }
    }
    
    for (i <- (text.length - 1) to 0 by -1) yield task {
      interpret(text.substring(i, text.length))
    }
    
    solutions.onKey(text) {
      v => println("Solutions: " + v)
    }
  }
  
  // TODO knapsack
  
}


trait IQueue[T] {
  
  def head: T
  
  def tail: IQueue[T]
  
  def foreach[U](f: T => U): Unit
  
  def seal(): IQueue[T]
  
}














