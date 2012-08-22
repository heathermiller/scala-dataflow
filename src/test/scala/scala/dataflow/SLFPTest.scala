package scala.dataflow

import pool._

object SLFPTest extends App {

  val n = 300
  val p = new pool.Linear[Int]()
  val b = p.builder
  
  p.foreach(p("Foreach"))
  p.map[Int](_ * 2).foreach(p("Map"))
  p.filter(_ % 2 == 0).foreach(p("Filter"))
  val fold = p.mapFold(0)(_ + _)(x => x)

  for (i <- 1 to n) { b += i }
  b.seal(n)

  p("Fold")(fold.blocking)
  
  def p(pref: String) =
    (x: Int) => println(pref + ": " + x)

}
