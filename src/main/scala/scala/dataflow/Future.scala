package scala.dataflow



import scala.collection.mutable.MutableList


class Future[T] {

  private var res: Option[T] = None
  private var cbs: MutableList[T => Unit] = new MutableList()

  def map[U](f: T => U): Future[U] = {
    val fut = new Future[U]
    registerCB(x => fut.complete(f(x)))
    fut
  }

  def flatMap[U](f: T => Future[U]): Future[U] = {
    val fut = new Future[U]
    registerCB { x => f(x).foreach(fut.complete _) }
    fut
  }

  def andThen[U](body: => U) = {
    val fut = new Future[T]
    registerCB { x =>
      body
      fut.complete(x)
    }
    fut
  }

  def foreach[U](f: T => U) {
    registerCB(x => f(x))
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

    // Execute callbacks
    cbs.foreach(f => f(v))

    // Release list
    cbs = null
  }

}
