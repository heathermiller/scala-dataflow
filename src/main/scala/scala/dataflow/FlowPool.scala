package scala.dataflow

import jsr166y._

trait FlowPool[T] {
  
  def builder: Builder[T]
  def doForAll[U](f: T => U): Future[Int]
  def mappedFold[U, V >: U](accInit: V)(cmb: (V,V) => V)(map: T => U): Future[(Int, V)]
  def newPool[S]: FlowPool[S]

  // Monadic Ops

  def exists(f: T => Boolean): Future[Boolean] = {
    val fut = new Future[Boolean]
    doForAll { x =>
      if (f(x)) fut.tryComplete(true)
    } map { c =>
      fut.tryComplete(false)
    }
    fut
  }

  def map[S](f: T => S): FlowPool[S] = {
    val fp = newPool[S]
    val b  = fp.builder

    doForAll { x =>
      b << f(x)
    } map { b.seal _ }

    fp
  }

  def foreach[U](f: T => U) { doForAll(f) }

  def fold[U >: T](z: U)(op: (U, U) => U): Future[U] =
    mappedFold(z)(op)(x => x).map(_._2) 

  def filter(f: T => Boolean): FlowPool[T] = {
    val fp = newPool[T]
    val b  = fp.builder

    mappedFold(0)(_ + _) { x =>
      if (f(x)) { b << x; 1 }
      else 0
    }  map {
      case (c,fc) => b.seal(fc)
    }

    fp
  }

  def flatMap[S](f: T => FlowPool[S]): FlowPool[S] = {
    val fp = newPool[S]
    val b  = fp.builder

    mappedFold(future(0))(futLift(_ + _)) { x =>
      val ifp = f(x)
      ifp.doForAll(b << _)
    } map { 
      case (c,cfut) => cfut.map(b.seal _)
    }

    fp
  }

}

object FlowPool {
  def BLOCKSIZE = 256
  def LAST_CALLBACK_POS = BLOCKSIZE - 3
  def MUST_EXPAND_POS = BLOCKSIZE - 2
  def IDX_POS = BLOCKSIZE - 1
  def MAX_BLOCK_ELEMS = LAST_CALLBACK_POS
  
  def newBlock(idx: Int, initEl: AnyRef = CallbackNil) = {
    val bl = new Array[AnyRef](BLOCKSIZE)
    bl(0) = initEl
    bl(MUST_EXPAND_POS) = MustExpand
    bl(IDX_POS) = idx.asInstanceOf[AnyRef]
    bl
  }
  
  val forkjoinpool = new ForkJoinPool
  
  def task[T](fjt: ForkJoinTask[T]) = Thread.currentThread match {
    case fjw: ForkJoinWorkerThread =>
      fjt.fork()
    case _ =>
      forkjoinpool.execute(fjt)
  }
}
