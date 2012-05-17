




# Flow-Pool Pseudocode



## Data-Types and Constants

	type Elem
	
    type Block {
	  array: Array
	  index: Int
	  blockindex: Int
	  next: Block
	}
	
	type FlowPool {
  	  start: Block
      current: Block
	}
	
	type Terminal {
	  sealed: Int
	  callbacks: List[Elem => Unit]
	}
	
	BLOCKSIZE = 256
	LASTELEMPOS = BLOCKSIZE - 2
	NOTSEALED = -1
	
	
## Operations


### Constructor

    def create()
	  new FlowPool {
	    start = createBlock(0)
		current = start
	  }
	
    def createBlock(bidx: Int)
      new Block {
	    array = new Array(BLOCKSIZE)
	    index = 0
	    blockindex = bidx
	    next = null
	  }

### Append (`<<`)
	
    def append(elem: Elem)
	  b = READ(current)
	  idx = READ(b.index)
	  nextobj = READ(b.array(idx + 1))
	  curobj = READ(b.array(idx))
	  if (check(b, idx, curobj, nextobj)) {
  	    if (CAS(b.array(idx + 1), nextobj, curobj)) {
 	      if (CAS(b.array(idx), curobj, elem)) {
		    WRITE(b.index, idx + 1)
			invokeCallbacks(elem, curobj)
		  } else append(elem)
	    } else append(elem)
	  } else {
	    advance()
		append(elem)
	  }
	
	def check(b: Block, idx: Int, curobj: Object, nextobj: Object)
      // The check on the index is done implicitly in the real code
	  if (idx > LASTELEMPOS) return false
	  else curobj match {
	    elem: Elem =>
		  return false
		term: Terminal =>
		  if (term.sealed == NOTSEALED) return true
		  else {
			if (totalElems(b, idx) < term.sealed) return true
			else error("sealed")
		  }
		null =>
		  error("unreachable")
	  }
	
	def advance()
	  b = READ(current)
	  idx = READ(b.index)
	  if (idx > LASTELEMPOS) expand(b)
	  else {
	    obj = READ(b.array(idx))
	    if (obj is Elem) WRITE(b.index, idx + 1)
	  }
	
	def expand(b: Block)
	  nb = READ(b.next)
	  if (nb is null) {
	    nb = createBlock(b.blockindex + 1)
	    if (CAS(b.next, null, nb)) CAS(current, b, nb)
        // In the real code we take a shortcut here (read, and try to CAS)
	  } else {
	    CAS(current, b, nb)
	  }
	
	def totalElems(b: Block, idx: Int)
	  return b.blockindex * BLOCKSIZE + idx
    
	def invokeCallbacks(elem: Elem, term: Terminal)
	  for (f <- term.callbacks) future {
	    f(elem)
	  }


### Seal

	def seal(size: Int)
	  b = READ(current)
	  idx = READ(b.index)
	  if (idx <= LASTELEMPOS) {
	    curobj = READ(b.array(idx))
	    curobj match {
		  term: Terminal =>
		    tryWriteSeal(term, b, idx, size)
		  elem: Elem =>
		    WRITE(b.index, idx + 1)
		    seal(size)
		  null =>
		    error("unreachable")
		}
	  } else {
        expand(b)
        seal(size)
      }
	
	def tryWriteSeal(term: Terminal, b: Block, idx: Int, size: Int)
	  val total = totalElems(b, idx)
	  if (total > size) error("too many elements")
	  if (term.sealed == NOTSEALED) {
	    nterm = new Terminal {
		  sealed = size
		  callbacks = term.callbacks
		}
	    CAS(b.array(idx), term, nterm)
	  } else if (term.sealed != size) {
	    error("already sealed with different size")
	  }


### doForAll

    def doForAll(f: Elem => Unit)
	  future {
	    asyncDoForAll(f, start, 0)
	  }
	
	def asyncDoForAll(f: Elem => Unit, b: Block, idx: Int)
	  if (idx <= LASTELEMPOS) {
        obj = READ(b.array(idx))
	    obj match {
	      term: Terminal =>
            if (term.sealed <= totalElems(b,idx))
               return totalElems(b,idx)

		    nterm = new Terminal {
			  sealed = term.sealed
			  callbacks = f :: term.callbacks
			}
		    if (!CAS(b.array(idx), term, nterm)) asyncForeach(f, b, idx)
		  elem: Elem =>
		    f(elem)
			asyncForeach(f, b, idx + 1)
		  null =>
		    error("unreachable")
		}
	  } else {
        // In the real code we take a shortcut when preparing the new block
	    expand(b)
		asyncForeach(f, b.next, 0)
	  }
	  
