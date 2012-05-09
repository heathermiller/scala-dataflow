
## Building 

Runs with SBT 0.11.x



## TODO list

### FlowPool implementation

- implement `seal`
- update `foreach` to return `Future[SealedPool[T]]`
- implement the `folding` projection
- `map`, `flatMap`, `filter`


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

