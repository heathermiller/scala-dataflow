
## Building 

Runs with SBT 0.11.x

## TODO list
- Prepare benchmark script (Alex)
- Change MLFP seal procedure w/ rehashing (Tobias)
- Write benchmarks (q/fp map bench, q reduce bench, q/fp hist bench parallelism) (Tobias)
- Write chapter in paper about benchmarks
- Write chapter in paper about MLFP

## DONE list

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


## Paper Outline

- Intro / Motivation (1.5 pages)
- Model of Computation (0.75 page) **DEP**
- Programming Model (2 pages) **DEP**
	- First intro to API
	- Small discussion and justification of API.
	    - why the foreach instead of blocking? blocking is bad in most
          programming models (JVM, CLR, most native platforms)
	    - why there is no foreach on a builder - would not be
          deterministic
		- why there is no append on a pool - the GC argument
		- Deterministic
		- Can run arbitrarily long (GC point)
		- how to build higher-level combinators (map, flatMap,
          reducing, generators, either, etc.)
	- Grammar (concise)
		- point out that builder was left out in the grammar, etc, as
          a memory hack i.e. implementation detail
- Implementation
	- Pseudocode (1 page)
	- Discussion of pseudocode (1 page)
- Proofs (2-2.5 pages) **DEP**
	- ...
- Experimental Results (2 pages)
	- Benchmarks, plots (Heather can generate plots)
- Related Work (2 pages)
	- Scala Futures
	- CnC
	- Sarkar's futures (Data-driven futures) Look at callback-related references
	- Twitter futures
	- Oz
	- gpars (dataflow stuff)
	- i-Structures (any other Arvind stuff)
	- Dataflow Java?
	- FlumeJava
	- M$ TPL?
	- CLQ
	- LTQ
	- Michael Scott CLQ
	- take a look at source of LTQ
	- research blocking queues
	- Nir Shavit, Data Structures in the Multicore Age
	- Introduction crap
		- the Art of Multiprocessor Programming, 2008, Herlihy, Shavit, 
		- Concurrent Data Structures, 2007, Moir, Shavit.
		- Linearizability: A Correctness Condition for Concurrent Objects, 1990, Herlihy
- Conclusion (0.25 page)
	- see need to embed callbacks within the data structure themselves
	- higher order combinators built on top of `<<` and `foreach`,
      just like in sequential collections
	- postulate a range of other collection-types in the
      single-assignment model, which haven't been addressed before
- Appendix A
  - Abstract pool semantics
  - Linearizability
  - Lock-freedom
- Appendix B
	- Calculus
	- Determinism Proof
- Appendix C
	- Extra plots (if they exist)

## Heather Philipp last-minute TODOs

- Heather needs to finish the plots
- Philipp needs to go through determinism proof 
- Heather needs to read through entire paper for typos
- Philipp also needs to read through entire paper for typos
- Heather needs to add summary of lock-freedom proof **DONE**
- Heather needs to fix point in the expand lemma of the lock-freedom proof
- Heather needs to summarize Comm size-scaling benchmark
- Philipp needs to figure out how to add the determinism syntax to the model of computation

