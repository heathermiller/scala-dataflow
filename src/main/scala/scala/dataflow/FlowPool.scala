package scala.dataflow



import jsr166y._



trait FlowPool[T] extends Builder[T] {
  
  def newPool[S]: FlowPool[S]
  
  def builder: Builder[T]
  
  def aggregate[S](zero: =>S)(cmb: (S, S) => S)(folder: (S, T) => S): Future[S]
  
  /* views */

  def diverger = new pool.Diverger[T](this)

  /* combinators */
  
  def mapFold[U, V >: U](zero: =>V)(cmb: (V, V) => V)(map: T => U) =
    aggregate(zero)(cmb)((acc, x) => cmb(acc, map(x)))
  
  def exists(p: T => Boolean): Future[Boolean] =
    aggregate(false)(_ || _) {
      (some, x) => some || p(x)
    }
  
  def forall(p: T => Boolean): Future[Boolean] =
    aggregate(true)(_ && _) {
      (all, x) => all && p(x)
    }
  
  def count(p: T => Boolean): Future[Int] =
    aggregate(0)(_ + _) {
      (cnt, x) => cnt + (if (p(x)) 1 else 0)
    }
  
  def ++[S >: T](that: FlowPool[S]): FlowPool[S] = {
    val fp = newPool[S]
    val b  = fp.builder
    
    for (x <- this) b += x
    for (y <- that) b += y
    
    fp
  }
  
  def filter(p: T => Boolean): FlowPool[T] = {
    val fp = newPool[T]
    val b  = fp.builder
    
    aggregate(0)(_ + _) {
      (acc, x) => if (p(x)) {
        b += x
        acc + 1
      } else acc + 0
    } map { b.seal(_) }
    
    fp
  }

  def flatMap[S](f: T => FlowPool[S]): FlowPool[S] = {
    val fp = newPool[S]
    val b  = fp.builder
    def futureAdd(f1: Future[Int], f2: Future[Int]) =
      for (a <- f1; b <- f2) yield a + b
    
    aggregate(future(0))(futureAdd) {
      (sf1, x) =>
      val sf2 = f(x).aggregate(0)(_ + _) {
        (acc, y) =>
        b += y
        acc + 1
      }
      futureAdd(sf1, sf2)
    }
    
    fp
  }

  def foreach[U](f: T => U): Future[Unit] =
    aggregate(())((_, _) => ()) {
      (acc, x) => f(x); ()
    }
  
  def fold[U >: T](z: U)(op: (U, U) => U): Future[U] =
    aggregate[U](z)(op)(op)

  def map[S](f: T => S): FlowPool[S] = {
    val fp = newPool[S]
    val b  = fp.builder

    aggregate(0)(_ + _) {
      (acc, x) =>
      b += f(x)
      acc + 1
    } map { b.seal _ }

    fp
  }

}


object FlowPool extends pool.Factory[FlowPool] {
  def BLOCKSIZE = 256
  def LAST_CALLBACK_POS = BLOCKSIZE - 3
  def MUST_EXPAND_POS = BLOCKSIZE - 2
  def IDX_POS = BLOCKSIZE - 1
  def MAX_BLOCK_ELEMS = LAST_CALLBACK_POS
  
  private[dataflow] def newBlock(idx: Int, initEl: AnyRef) = {
    val bl = new Array[AnyRef](BLOCKSIZE)
    bl(0) = initEl
    bl(MUST_EXPAND_POS) = pool.MustExpand
    bl(IDX_POS) = idx.asInstanceOf[AnyRef]
    bl
  }
  
  val forkjoinpool = new ForkJoinPool
  
  def apply[T]() = linear[T]

  def linear[T] = new pool.Linear[T]
  
  def multi[T](lanes: Int) = new pool.MultiLane[T](lanes)
  
  def task[T](fjt: ForkJoinTask[T]) = Thread.currentThread match {
    case fjw: ForkJoinWorkerThread =>
      fjt.fork()
    case _ =>
      forkjoinpool.execute(fjt)
  }

}












