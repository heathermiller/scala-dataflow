# Flow Pool Pseudocode

## Cell states
* full
* sealed
* free
* callbacks

## Write / Seal
    sub write(val,i) {
        // Try to write element
        nvobj = { state = full, val = val, cbs = Nil }
        do {
            do {
                  cobj = elems[i]
                  if (cobj->state != full) break;
                  i++
            } 
            if (cobj->state == sealed) fail("Insert into sealed pool")
        } while(!propagateCallbacks(cobj, i+1) || !elems[i].cas(cobj, nvobj))

        // Call callbacks
        cobj->cbs.foreach(cb => cb(val))
    }

## Propagate Callbacks
    sub propCallback(cobj, i) {
        do {
            cnobj = elems[i];
            if (cnobj != cobj) {
                // Element is in bad state
                if (!cnobj->state == free) {
                    // We end up here if somebody else has already written.
                    // Possible cases:
                    // cnobj->state in {full,sealed}
                    //   ==> pool advanced
                    // cnobj->state == callbacks && cnobj != cobj
                    //   ==> somebody else propagated callbacks AND they
                    //       have already been altered. This requires
                    //       elems[i]->state == full
                    return false;
                }
                // try to set elemnt
                ok = !elems[i+1].cas(cnobj,cobj);
            } else ok = true;
        while (!ok);     
    }


## Add Callback
    sub addCallback(cb,i) {
        do {
            while true {
                cobj = elems[i]
                if (cobj->state != full) break;
                cb(cobj->val); i++
            }
            if (cobj->state == sealed) { cb(end); return; }
            cblist = cb :: (cobj->state == free ? Nil : cobj->cbs)
            nobj = { state = callback, val = null, cbs = cblist }
        } while(!elems[i].cas(cobj, nobj));
    }

## Add new block