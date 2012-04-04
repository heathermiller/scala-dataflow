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
  
  def producerConsumerBlocking() {
    val stream = FlowQueue[Int]()
    
    val producer = task {
      def produce100(fs: FlowQueue[Int], i: Int) {
        if (i == 100) fs.seal()
        else {
          fs << i
          produce100(fs.blocking.tail, i + 1)
        }
      }
      produce100(stream, 0)
    }
    
    val consumer = task {
      def consume(fs: FlowQueue[Int]) {
        if (!fs.blocking.isEmpty) {
          println(fs.blocking.head)
          consume(fs.blocking.tail)
        } else println("done!")
      }
      consume(stream)
    }
    
    val matchingConsumer = task {
      def consume(fs: FlowQueue[Int]): Unit = fs.blocking match {
        case c << cs =>
          println(c)
          consume(cs)
        case Seal() =>
          println("done")
      }
      consume(stream)
    }
  }
  
  def producerConsumerMonadic() {
    val stream = FlowQueue[Int]()
    
    val producer = task {
      def produce100(fs: FlowQueue[Int], i: Int) {
        if (i == 100) fs.seal()
        else {
          fs << i
          produce100(fs.blocking.tail, i + 1)
        }
      }
      produce100(stream, 0)
    }
    
    val consumer = task {
      def consume(fs: FlowQueue[Int]): Unit = fs onBind {
        case c << cs =>
          println(c)
          consume(cs)
        case Seal() =>
          println("done")
      }
      consume(stream)
    }
    
    val foreachConsumer = task {
      stream foreach {
        println
      } andThen {
        println("done")
      }
    }
  }
  
}
