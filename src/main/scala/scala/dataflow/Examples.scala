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
    val channel = FlowBuffer[Int]()
    
    val producer = task {
      for (i <- 0 until 100) channel << i
      channel.seal()
    }
    
    val consumer = task {
      channel foreach {
        println
      } andThen {
        println("done")
      }
    }
  }
  
  def producerConsumerBlocking() {
    val channel = FlowBuffer[Int]()
    
    val producer = task {
      for (i <- 0 until 100) channel << i
      channel.seal()
    }
    
    val consumer = task {
      for (x <- channel.blocking) println(x)
      println("done")
    }
    
    val readerConsumer = task {
      val reader = channel.reader.blocking
      while (!reader.isEmpty) println(reader.pop())
      println("done")
    }
  }
  
  def producerConsumerStreamBlocking() {
    val channel = FlowStream[Int]()
    
    val producer = task {
      def produce(ch: FlowStream[Int], i: Int) {
        produce(ch << i, i + 1)
      }
      produce(channel, 0)
    }
    
    val consumer = task {
      def consume(channel: FlowStream.Blocking[Int]): Unit = channel match {
        case c << ch =>
          println(c)
          consume(ch.blocking)
        case Seal() =>
          println("done")
      }
      consume(channel.blocking)
    }
  }
  
  def producerConsumerStreamMonadic() {
    val channel = FlowStream[Int]()
    
    val producer = task {
      def produce(ch: FlowStream[Int], i: Int) {
        produce(ch << i, i + 1)
      }
      produce(channel, 0)
    }
    
    val consumer = task {
      def consume(channel: FlowStream[Int]): Unit = channel onReady {
        case c << ch =>
          println(c)
          consume(ch)
        case Seal() =>
          println("done")
      }
    }
  }
  
  def dataflowList() {
    trait FlowList[T]
    
    case class Node[T](head: T, tail: FlowVar[FlowList[T]]) extends FlowList[T] {
      def this(head: T) = this(head, FlowVar())
    }
    
    case class End[T]() extends FlowList[T]
    
    val list = new Node(1)
    
    val producer = task {
      def produce(ch: FlowList[Int]): Unit = ch match {
        case Node(head, tail) =>
          tail << new Node(head + 1)
          produce(tail.blocking())
      }
      produce(list)
    }
    
    val consumer = task {
      def consume(ch: FlowList[Int]): Unit = ch match {
        case Node(head, tail) =>
          println(head)
          consume(tail.blocking())
        case End() =>
          println("done")
      }
    }
  }

  def waveFrontBlocking() {
    val surface = FlowArray[Int](10,10)
    def calculate(x: Int, y: Int) = {
      assert(x > 0)
      assert(y > 0)
      val sum =
        surface.blocking((x-1,y)) +
        surface.blocking((x,y-1)) +
        surface.blocking((x-1,y-1))

      surface << ((x,y),sum) 
    }

    // Now I need to think really hard how to partition the wavefront
    // correctly. Should I need to do so at this point?
    
  }
  
  def boundedProducerConsumer() {
    // TODO
  }
  
  def knapsackProblem() {
    // TODO
  }
  
  def tweetTrending() {
    // TODO
  }
  
  def inversePermutation() {
    val n = 10
    val x = FlowArray[Int](n)
    val y = FlowArray[Int](n)

    task {
      for (i <- 0 to n-1) {
        x << (i,(i + 4) % n);
      }
    }

    task {
      x.zipWithIndex foreach {
        case (i,v) => y << (v,i)
      }
    }
  }
  
  def histogram() {
    val n = 10000
    val buckets = 10
    val maxval = 100

    val buf = FlowBuffer[Double]()
    def calculate(i: Int) = {
      // Do some complicated calculations
      val res = math.sqrt(i)
      buf << res
    }

    // Seal buffer after all calculations
    buf.sealAfter(n)

    for (i <- 1 to n) yield task { calculate(i) }
    
    // Merger
    val merger = task { 
      val hist = Array.fill(buckets)(0)
      for (e <- buf.blocking) {
        val bi = math.floor(e / maxval * buckets)
        hist(bi.toInt) = hist(bi.toInt) + 1
      }
    }
  }
  
  def partitioning() {
    // TODO
  }
  
}


object Scratchpad {
  
}
























