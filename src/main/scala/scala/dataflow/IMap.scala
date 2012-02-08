package scala.dataflow






trait IMap[K, V] {
  
  def apply(key: K): V
  
  def update(key: K, value: V): Unit
  
  def seal(): IMap[K, V]
  
}


object IMap {
  
  def apply[K, V]: IMap[K, V] = null
  
  def dynamicProgramming() {
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
        solution <- solutions(suffix.substring(length, suffix.length))
      } yield firstword :: solution
      
      solutions(suffix) = possibilities
    }
    
    for (i <- (text.length - 1) to 0 by -1) yield thread {
      interpret(text.substring(i, text.length))
    }
    
    solutions(text)
  }
  
}
