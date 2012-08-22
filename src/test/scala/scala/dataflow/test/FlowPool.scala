package scala.dataflow
package test






object FlowPoolTests {
  
  def unfoldpool() {
    val fp = seal {
      val fp = FlowPool.linear[Int]

      fp += 1

      for (x <- fp) {
        if (x < 100) fp += x + 1
      }

      fp
    }
  }

}