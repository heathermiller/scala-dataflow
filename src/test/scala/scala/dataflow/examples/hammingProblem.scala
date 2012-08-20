package scala.dataflow
package examples



import FlowPool._



trait Hamming extends testing.Benchmark with Utils.Props {
  
  def factory: FlowFactory[FlowPool]

  def newPool[T]: FlowPool[T]
  
  val greatest = sys.props("greatest").toInt
  
  val primelist: Array[Int] = {
    def isPrime(x: Int) = (2 until (math.sqrt(x) + 1).toInt).forall(x % _ != 0)
    val arr = new Array[Int](size)
    var total = 0
    var i = 2
    while (total < size) {
      if (isPrime(i)) {
        arr(total) = i
        total += 1
      }
      i += 1
    }
    arr
  }
  
  def run() {
    val primes = newPool[Int]

    Utils.task {
      for (i <- 0 until size) primes += primelist(i)
      primes.seal(size)
    }

    val output = factory.unfold(1) { num =>
      for {
        p <- primes
        val multiple = num * p
        if multiple <= greatest
      } yield multiple
    }
  }

}


object LinearFlowPoolHamming extends Hamming {
  
  def newPool[T] = FlowPool.linear[T]
  
  def factory = pool.Linear

}


object MultiFlowPoolHamming extends Hamming {
  
  def newPool[T] = FlowPool.multi[T](lanes)
  
  def factory = pool.MultiLane

}
