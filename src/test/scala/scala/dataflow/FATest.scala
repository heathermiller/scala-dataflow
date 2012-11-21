package scala.dataflow

import array._

object FATest extends App {

  /*
  val n = 10000

  val fa1 = FlowArray.tabulate(n)(x => x*x toLong)
  /*
  val fa2 = fa1.flatMapN(n)(mkFA _)
  val fa3 = fa2.map(_ / 2)
  val fut = fa3.fold(0L)(_ + _)
  */
  val fut = fa1.fold(0L)(_ + _)

  println(fut.blocking)

  def mkFA(v: Long) = {
    FlowArray.tabulate(n)(x => x*v)
  }
  */

  val fa = (FlowArray.tabulate(200)(x => x * 100) flatMapN 100) { x =>
    FlowArray.tabulate(100)(y => x + y)
  }

  val res = fa.blocking

}