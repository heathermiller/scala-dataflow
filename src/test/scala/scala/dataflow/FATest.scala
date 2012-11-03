package scala.dataflow

import array._

object FATest extends App {

  val raw = Array.tabulate(20)(x => x*x toDouble)

  val fa1 = new FlowArray(raw)
  val fa2 = fa1.converge { x =>
    println("Test: " + x)
    x <= 1
  } { x => 
    println("Halve: " + x)
    x / 2
  }
  val f   = fa2.fold(0.0) { (x,y) =>
    println("Sum: %f + %f".format(x,y))
    x + y
  }                        

  println(f.blocking)

}
