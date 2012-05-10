
## Building 

Runs with SBT 0.11.x



## TODO list

### FlowPool implementation

- implement `seal` - put the seal at the position where it needs to
  be, if its in the next block, set the pointer appropriately, but
  don't eagerly allocate all the blocks
- try to optimize to get closer to CLQ
- update `foreach` to return `Future[Unit]`
- dummy `future` with `map`
- implement the `folding` projection
- `map`, `flatMap`, `filter`
- scheduler and coroutine-like callbacks - ALEX


### Benchmarks

- comparison with CLQ
- beyond that - try to find a simple application with `map`s (we need
  a use-case for multiple consumers and one, or a few, producers)


### Proofs

- abstract pool semantics
- linearizability
- lock-freedom
- determinism


### Paper

