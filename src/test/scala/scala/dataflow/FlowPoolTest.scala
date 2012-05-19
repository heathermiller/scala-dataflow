package scala.dataflow

import java.lang.Integer

object FlowPoolTest extends App {

  val n = 300
  val pool = new FlowPool[Integer]()
  val b = new Builder[Integer](pool.initBlock)
  
  pool.foreach(p("Foreach"))
  pool.map[Integer](_*2).foreach(p("Map"))
  pool.filter(_ % 2 == 0).foreach(p("Filter"))
  val fold = pool.mappedFold(0)(_ + _)(x => x)

  for (i <- 1 to n) { b << i }
  b.seal(n)

  p("Fold")(fold.blocking._2)
  
  def p(pref: String) =
    (x: Integer) => println(pref + ": " + x)

}


