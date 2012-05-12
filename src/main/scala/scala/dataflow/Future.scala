package scala.dataflow

import scala.collection.mutable.MutableList

// TODO do we need CAS here or are synchronized methods enough? -- tobias
class Future[T] {

  var res: Option[T] = None
  var cbs: MutableList[T => Unit] = new MutableList()

  def map[U](f: T => U): Future[U] = {
    val fut = new Future[U]
    registerCB(x => fut.complete(f(x)))
    fut
  }

  private def registerCB(f: T => Unit) {
    synchronized {
      res match {
        case Some(r) => f(r)
        case None    => cbs += f
      }
    }
  }
  
  private[dataflow] def complete(v: T) {
    synchronized {
      res match {
        case Some(_) => sys.error("Future completed twice")
        case None    => res = Some(v)
      }
    }
    
    // TODO ALEX invoke scheduler with cbs
    cbs = null  // Release list
  }

}
