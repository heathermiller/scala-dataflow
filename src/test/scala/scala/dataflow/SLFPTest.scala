package scala.dataflow

object SLFPTest extends App {

  val n = 300
  val pool = new SingleLaneFlowPool[Int]()
  val b = pool.builder
  
  pool.foreach(p("Foreach"))
  pool.map[Int](_*2).foreach(p("Map"))
  pool.filter(_ % 2 == 0).foreach(p("Filter"))
  val fold = pool.mapFold(0)(_ + _)(x => x)

  for (i <- 1 to n) { b << i }
  b.seal(n)

  p("Fold")(fold.blocking)
  
  def p(pref: String) =
    (x: Int) => println(pref + ": " + x)

}
