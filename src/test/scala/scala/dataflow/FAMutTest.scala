package scala.dataflow

import array._

object FAMutTest extends App {

  val maxVal = 1000000
  val size = math.ceil(math.log(maxVal) / math.log(2)).toInt + 1

  val fa1 = FlowArray.tabulate(maxVal)(x => x)
  val fa2 = fa1.mutConverge(x => new HIter(x))(x => x)(_.v <= 1)(x => x.adv(x.v / 2))
  val fa3 = fa2.map(_.dat)
  val f   = fa3.fold(Array.fill(size)(0.0)) { (x1, x2) =>
    (x1 zip x2) map {
      case (x,y) => x + y
    }
  }

  for (i <- f.blocking) {
    println(i)
  }


  class HIter(x: Double) {
    var i = 0
    val dat: Array[Double] = new Array[Double](size)

    def v = dat(i)
    def adv(v: Double) = {
      i = i+1
      dat(i) = v
    }
    
    dat(0) = x
  }

}
