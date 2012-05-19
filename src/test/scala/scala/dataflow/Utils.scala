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
    lazy val par = sys.props("par").toInt
  }
  
}
