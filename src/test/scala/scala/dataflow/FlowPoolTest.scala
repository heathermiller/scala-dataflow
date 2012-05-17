package scala.dataflow

import java.lang.Integer

object FlowPoolTest extends App {

  val n = 10
  val pool = new FlowPool[Integer]()
  val b = new Builder[Integer](pool.initBlock)
  
  pool.foreach(p("Foreach"))
  pool.map[Integer](_*2).foreach(p("Map"))
  pool.map[Integer](_*3).foreach(p("Map"))
  pool.filter(_ % 2 == 0).foreach(p("Filter"))
  pool.mappedFold(0)(_ + _)(x => x).map {
    case (cnt,v) => p("Fold")(v)
  }

  for (i <- 1 to n) { b << i }
  b.seal(n)
  
  Thread.sleep(100)
  
  def p(pref: String) =
    (x: Integer) => println(pref + ": " + x)

}


