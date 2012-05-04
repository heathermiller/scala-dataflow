# Flow Pool Pseudocode

## Cell states
* full
* sealed
* free
* callbacks

## Write / Seal
    sub write(val,i) {

        // Create new element
        nvobj = { state = full, val = val, cbs = Nil }

        // Try to write element
        do {
            // Advance to next non-full
            do cobj = elems[i];
            while (cobj->state == full && ++i);

            // Are we sealed?
            if (cobj->state == sealed)
               fail("Insert into sealed pool")

        } while (!propagateCallbacks(cobj, i+1) ||
                 !elems[i].cas(cobj, nvobj))

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
                    //       that elems[i-1]->state == full
                    return false;
                }
                // try to set elemnt
                ok = elems[i+1].cas(cnobj,cobj);
            } else ok = true;
        } while (!ok);     
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
Called implicitly when advance reaches end of block

    sub nextBlock(end) {
        nptr = end->next
        if (!nptr) {
           nblock = new Block();
           end->next.cas(nptr, nblock)
        }
        return end->next;
    }