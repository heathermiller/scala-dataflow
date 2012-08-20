package scala.dataflow






object HashTable extends testing.Benchmark with Utils.Props {
  import Utils._
  
  var table = new hashtable.FlowHashTable[Data, Data]()
  val data = new Array[Data](size)
  for (i <- 0 until size) data(i) = new Data(i)
  
  class Inserter(val idx: Int, val sz: Int) extends Thread {
    override def run() {
      var i = idx * sz
      val until = idx * sz + sz
      while (i < until) {
        table(data(i)) = data(i)
        i += 1
      }
    }
  }
  
  override def setUp() {
    table = new hashtable.FlowHashTable()
  }
  
  def run() {
    val work = size / par
    val threads = for (idx <- 0 until par) yield new Inserter(idx, work)
    
    threads.foreach(_.start())
    threads.foreach(_.join())
  }
  
  override def tearDown() {
    //println(table)
  }
  
}
