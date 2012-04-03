package scala.dataflow






trait FlowMap[K, V] {
  
  def value(key: K): V
  
  def apply(key: K): Future[V]
  
  def update(key: K, value: V): Unit
  
  def seal(): FlowMap[K, V]
  
  def foreach[U](f: (K, V) => U): Unit
  
  def onKey[U](key: K)(body: V => U): Unit
  
}


trait Future[T] {
  def foreach[U](f: T => U): Unit
  
  def andThen[U](body: =>U): Future[T]
}


object FlowMap {
  
  def apply[K, V]: FlowMap[K, V] = null
  
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
  
}


trait FlowStream[T] {
  
  def head: T
  
  def tail: FlowStream[T]
  
  def isEmpty: Boolean
  
  def <<(x: T): FlowStream[T]
  
  def foreach[U](f: T => U): Future[T]
  
  def seal(): FlowStream[T]
  
  def onBind[U](fs: FlowStream[T] => U): Unit
  
}


object << {
  
  def unapply[T](fs: FlowStream[T]): Option[(T, FlowStream[T])] =
    if (fs.isEmpty) None
    else Some((fs.head, fs.tail))
  
}


object Seal {
  
  def unapply[T](fs: FlowStream[T]): Boolean = fs.isEmpty
  
}


object FlowStream {
  
  def apply[T](): FlowStream[T] = null
  
  def producerConsumerBlocking() {
    val channel = FlowStream[Int]()
    
    val producer = task {
      def produce100(ch: FlowStream[Int], i: Int) {
        if (i == 100) ch.seal()
        else {
          ch << i
          produce100(ch.tail, i + 1)
        }
      }
      produce100(channel, 0)
    }
    
    val consumer = task {
      def consume(ch: FlowStream[Int]) {
        if (!ch.isEmpty) {
          println(ch.head)
          consume(ch.tail)
        } else println("done!")
      }
      consume(channel)
    }
    
    val matchingConsumer = task {
      def consume(ch: FlowStream[Int]): Unit = ch match {
        case c << cs =>
          println(c)
          consume(cs)
        case Seal() =>
          println("done")
      }
      consume(channel)
    }
  }
  
  def producerConsumerMonadic() {
    val channel = FlowStream[Int]()
    
    val producer = task {
      def produce100(ch: FlowStream[Int], i: Int) {
        if (i == 100) ch.seal()
        else {
          ch << i
          produce100(ch.tail, i + 1)
        }
      }
      produce100(channel, 0)
    }
    
    val consumer = task {
      def consume(ch: FlowStream[Int]): Unit = ch onBind {
        case c << cs =>
          println(c)
          consume(cs)
        case Seal() =>
          println("done")
      }
      consume(channel)
    }
    
    val foreachConsumer = task {
      channel foreach {
        println
      } andThen {
        println("done")
      }
    }
  }
  
}













