package scala.dataflow



import impl._



object FlowPoolBench2 extends testing.Benchmark with Utils.Props {
  import Utils._
  
  override def run() {
    val p = new pool.Linear[Data]()
    //val builder = pool.builder
    val builder = new pool.Linear.Builder[Data](p.initBlock)
    val work = size
    val data = new Data(0)
    var i = 0
    
    while (i < work) {
      builder += data
      i += 1
    }
  }
  
}


object FlowPoolBenchSealed extends testing.Benchmark with Utils.Props {
  import Utils._
  
  override def run() {
    val p = new pool.Linear[Data]()
    //val builder = p.builder
    val builder = new pool.Linear.Builder[Data](p.initBlock)
    val work = size
    builder.seal(work)
    val data = new Data(0)
    var i = 0
    
    while (i < work) {
      builder += data
      i += 1
    }
  }
  
}

