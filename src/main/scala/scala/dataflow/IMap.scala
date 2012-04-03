package scala.dataflow






trait FlowMapLike[K, V, Async[X]] {
  
  def apply(key: K): Async[V]
  
  def get(key: K): Async[Option[V]]
  
  def update(key: K, value: V): Unit
  
  def seal(): FlowMap.Blocking[K, V]
  
  def foreach[U](f: (K, V) => U): Future[Unit]
  
  def onKey[U](key: K)(body: V => U): Unit
  
  def blocking: FlowMap.Blocking[K, V]
  
}


trait FlowMap[K, V] extends FlowMapLike[K, V, Future]


trait Future[T] {
  def foreach[U](f: T => U): Unit
  
  def andThen[U](body: =>U): Future[T]
}


object FlowMap extends FlowMapFactory[FlowMapLike] {
  
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
  
}


trait FlowStreamLike[T, Async[X]] {
  
  def head: Async[T]
  
  def tail: Async[FlowStream[T]]
  
  def isEmpty: Async[Boolean]
  
  def <<(x: T): FlowStream.Blocking[T]
  
  def foreach[U](f: T => U): Future[Unit]
  
  def seal(): FlowStream.Blocking[T]
  
  def onBind[U](fs: FlowStream.Blocking[T] => U): Unit
  
  def blocking: FlowStream.Blocking[T]
  
}


trait FlowStream[T] extends FlowStreamLike[T, Future]


object << {
  
  def unapply[T](fs: FlowStream.Blocking[T]): Option[(T, FlowStream[T])] =
    if (fs.isEmpty) None
    else Some((fs.head, fs.tail))
  
}


object Seal {
  
  def unapply[T](fs: FlowStream.Blocking[T]): Boolean = fs.isEmpty
  
}


trait FlowFactory[Flow[X, Z[_]]] {
  
  type Blocking[T] = Flow[T, Id]
  
}


trait FlowMapFactory[Flow[X, Y, Z[_]]] {
  
  type Blocking[K, V] = Flow[K, V, Id]
  
}


object FlowStream extends FlowFactory[FlowStreamLike] {
  
  def apply[T](): FlowStream[T] = null
  
  def producerConsumerBlocking() {
    val stream = FlowStream[Int]()
    
    val producer = task {
      def produce100(fs: FlowStream[Int], i: Int) {
        if (i == 100) fs.seal()
        else {
          fs << i
          produce100(fs.blocking.tail, i + 1)
        }
      }
      produce100(stream, 0)
    }
    
    val consumer = task {
      def consume(fs: FlowStream[Int]) {
        if (!fs.blocking.isEmpty) {
          println(fs.blocking.head)
          consume(fs.blocking.tail)
        } else println("done!")
      }
      consume(stream)
    }
    
    val matchingConsumer = task {
      def consume(fs: FlowStream[Int]): Unit = fs.blocking match {
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
    val stream = FlowStream[Int]()
    
    val producer = task {
      def produce100(fs: FlowStream[Int], i: Int) {
        if (i == 100) fs.seal()
        else {
          fs << i
          produce100(fs.blocking.tail, i + 1)
        }
      }
      produce100(stream, 0)
    }
    
    val consumer = task {
      def consume(fs: FlowStream[Int]): Unit = fs onBind {
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


trait FlowVarLike[T, Async[X]] {
  
  def <<(x: T): Unit
  
  def apply(): Async[T]
  
  def blocking: FlowVar.Blocking[T]
  
}


trait FlowVar[T] extends FlowVarLike[T, Future]


object FlowVar extends FlowFactory[FlowVarLike] {
  
  def apply[T](): FlowVar[T] = null
  
}







