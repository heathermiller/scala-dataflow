package scala.dataflow






object Examples {
  
  def dynamicProgrammingBlocking() {
    val dictionary = Set("go", "got", "here", "there")
    val longest = dictionary.maxBy(_.length).length
    val text: String = "gothere"
    val solutions = FlowMap[String, Seq[List[String]]]
    solutions("") = Nil
    
    def interpret(suffix: String) {
      val possibilities = for {
        length <- 0 until (longest min suffix.length)
        val firstword = suffix.substring(0, length)
        if dictionary(firstword)
        solution <- solutions.blocking(suffix.substring(length, suffix.length))
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
    val solutions = FlowMap[String, Seq[List[String]]]
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
  
  def producerConsumerMonadic() {
    val queue = FlowQueue[Int]()
    
    val producer = task {
      for (i <- 0 until 100) queue << i
      queue.seal()
    }
    
    val consumer = task {
      queue foreach {
        println
      } andThen {
        println("done")
      }
    }
  }
  
  def producerConsumerBlocking() {
    val queue = FlowQueue[Int]()
    
    val producer = task {
      for (i <- 0 until 100) queue << i
      queue.seal()
    }
    
    val consumer = task {
      for (x <- queue.blocking) println(x)
      println("done")
    }
    
    val readerConsumer = task {
      val reader = queue.reader.blocking
      while (!reader.isEmpty) println(reader.pop())
      println("done")
    }
  }
  
  def dataflowList() {
    // TODO
  }
  
  def knapsackProblem() {
    // TODO
  }
  
  def boundedProducerConsumer() {
    // TODO
  }
  
}
