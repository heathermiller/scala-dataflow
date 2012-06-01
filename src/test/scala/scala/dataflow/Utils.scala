package scala.dataflow






object Utils {
  
  final class Data(val i: Int) {
    override def hashCode = i * 0x9e3775cd
    override def equals(other: Any) = other match {
      case d: Data => d.i == this.i
      case _ => false
    }
    override def toString = "Data(%d)".format(i)
  }
  
  trait Props {
    lazy val size = sys.props("size").toInt
    lazy val par = {
      val tmp = sys.props("par").toInt
      if (size % tmp == 0) tmp
      else sys.error("size not divisible by par")
    }
      
    lazy val lanes = {
      val sl = sys.props("lanes")
      if (sl(0) != 'x') sl.toInt
      else (par * sl.tail.toDouble).toInt
    }
  }

  def task(f: => Unit) = {
    val t = new Thread(new Runnable() { def run() = f })
    t.start()
    t
  }
  
}
