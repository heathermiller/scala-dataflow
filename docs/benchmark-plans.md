# Benchmark Plans

Parameters we can change are listed below.

## * parallelism (8)
1 To 64 insertion threads for both SLFP and MLFP i.e. (1,2,4,8,16,32,48,64)

## Platforms
- maglite 1x Niagara2 (8 cores, 64 HT)
- wolf    4x Intel i7 (32 cores, 64 HT)
- chara   1x Intel i7 (4 cores, 8 HT)

## Lanes (5)
x1, x1.5, x2, x3, x4

## Targets (4)
- SLFP
- MLFP
- LTQ
- CLQ (for Insert)

## Benchmarks (6)
- HistBench
- InsertBench
- ReduceBench
- SealedInsertBench
- UnsafeHistBench
- MapBench

## * sizes (3)
Try Them out!!

## N
15


# * parameter ranges estimations

Some parameter ranges might not be feasible - they last too long.

## wolf

### insert
20, drop first 5

SLFPInsertBench
* par - 1,2,4,8
* size - 2000000, 10000000, 15000000

MLFPInsertBench
* par - 1,2,4,8,16,32,64
* size - 2000000, 10000000, 15000000

CLQInsertBench
* par - 1,2,4,8
* size - 2000000, 10000000, 15000000

LTQInsertBench
* par - 1,2,4,8
* size - 2000000, 10000000, 15000000


### reduce
20, drop first 5

SFLPReduceBench
* par - 1,2,4,8
* size - 2000000, 5000000, 10000000

MLFPReduceBench
* par - 1,2,4,8,16,32,64
* size - 2000000, 5000000, 10000000

LTQReduceBench
* par - 1,2,4,8
* size - 2000000, 5000000, 10000000


### map
20, drop first 5

SFLPMapBench
* par - 1,2,4,8
* size - 2000000, 5000000, 10000000

MLFPMapBench
* par - 1,2,4,8,16,32,64
* size - 2000000, 5000000, 10000000

LTQMapBench
* par - 1,2,4,8
* size - 2000000, 5000000, 10000000



### histogram
20, drop first 5

SLFPHistBench
* par - 1,2,4,8,16
* size - 500000, 1000000

MLFPHistBench
* par - 1,2,4,8,16,32
* size - 500000, 1000000

LTQHistBench
* par - 1,2,4,8,16
* size - 500000, 1000000



### communication
20, drop first 5

SLFPCommBench
* par - 1
* size - 50000000

MLFPCommBench
* par - 1,2,4,8,16,32,64
* size - 50000000

LTQCommBench
* par - 1,2,4
* size - 50000000




## maglite


### insert
15, drop first 5

SLFPInsertBench
* par - 1,2,4,8,16
* size - 1000000, 2000000, 5000000, 10000000

MLFPInsertBench
* par - 1,2,4,8,16,32,64
* size - 1000000, 2000000, 5000000, 10000000

CLQInsertBench
* par - 1,2,4,8,16,32
* size - 1000000, 2000000, 5000000, 10000000

LTQInsertBench
* par - 1,2,4,8,16,32
* size - 1000000, 2000000, 5000000, 10000000


### reduce
15, drop first 5

SFLPReduceBench
* par - 1,2,4,8,16,32,64
* size - 1000000, 2000000, 5000000, 10000000

MLFPReduceBench
* par - 1,2,4,8,16,32,64
* size - 1000000, 2000000, 5000000, 10000000

LTQReduceBench
* par - 1,2,4,8,16,32,64
* size - 1000000, 2000000, 5000000, 10000000


### map
15, drop first 5

SFLPMapBench
* par - 1,2,4,8,16,32,64
* size - 1000000, 2000000, 5000000

MLFPMapBench
* par - 1,2,4,8,16,32,64
* size - 1000000, 2000000, 5000000

LTQMapBench
* par - 1,2,4,8,16,32,64
* size - 1000000, 2000000, 5000000


### histogram
15, drop first 5

SLFPHistBench
* par - 1,2,4,8,16,32
* size - 500000, 1000000

MLFPHistBench
* par - 1,2,4,8,16,32
* size - 500000, 1000000

LTQHistBench
* par - 1,2,4,8,16,32
* size - 500000, 1000000


### communication
15, drop first 5

SLFPCommBench
* par - 1
* size - 100000000

MLFPCommBench
* par - 1,2,4,8,16,32,64
* size - 100000000

LTQCommBench
* par - 1,2,4
* size - 100000000



## chara/lampmac14


### insert
20, drop first 5

SLFPInsertBench
* par - 1,2,4,8
* size - 2000000, 10000000, 15000000

MLFPInsertBench
* par - 1,2,4,8
* size - 2000000, 10000000, 15000000

CLQInsertBench
* par - 1,2,4,8
* size - 2000000, 10000000, 15000000

LTQInsertBench
* par - 1,2,4,8
* size - 2000000, 10000000, 15000000


### reduce
20, drop first 5

SFLPReduceBench
* par - 1,2,4,8
* size - 2000000, 5000000, 10000000

MLFPReduceBench
* par - 1,2,4,8
* size - 2000000, 5000000, 10000000

LTQReduceBench
* par - 1,2,4,8
* size - 2000000, 5000000, 10000000


### map
20, drop first 5

SFLPMapBench
* par - 1,2,4,8
* size - 2000000, 5000000, 10000000

MLFPMapBench
* par - 1,2,4,8
* size - 2000000, 5000000, 10000000

LTQMapBench
* par - 1,2,4,8
* size - 2000000, 5000000, 10000000



### histogram
20, drop first 5

SLFPHistBench
* par - 1,2,4,8
* size - 1500000, 4000000

MLFPHistBench
* par - 1,2,4,8
* size - 1500000, 4000000

LTQHistBench
* par - 1,2,4,8
* size - 1500000, 4000000



### communication
20, drop first 5

SLFPCommBench
* par - 1,2,4,8
* size - 100000000

MLFPCommBench
* par - 1,2,4,8
* size - 100000000

LTQCommBench
* par - 1,2,4,8
* size - 100000000

