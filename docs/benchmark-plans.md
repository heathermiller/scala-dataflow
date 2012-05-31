# Benchmark Plans

## Parallelism (8)
1 to 64 insertion threads for both SLFP and MLFP i.e. (1,2,4,8,16,32,48,64)

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

## Sizes (3)
Try them out!!

## N
15


# Parameter ranges estimations

Some parameter ranges might not be feasible - they last too long.

## wolf

### insert
15, drop first 5

SLFPInsertBench
par - 1,2,4,8
size - 1000000, 10000000, 15000000

MLFPInsertBench
par - 1,2,4,8,16,32,48,64
size - 1000000, 10000000, 15000000

CLQInsertBench
par - 1,2,4,8
size - 1000000, 10000000, 15000000

LTQInsertBench
par - 1,2,4,8
size - 1000000, 10000000, 15000000


### reduce
15, drop first 5

SFLPReduceBench
par - 1,2,4,8
size - 1000000, 5000000, 10000000

MLFPReduceBench
par - 1,2,4,8,16,32,48,64
size - 1000000, 5000000, 10000000

LTQReduceBench
par - 1,2,4,8
size - 1000000, 5000000, 10000000


### map
15, drop first 5



### histogram
15, drop first 5



### communication
4, drop first 1




## maglite


### insert
15, drop first 5

SLFPInsertBench
par - 1,2,4,8,16
size - 1000000, 2000000, 5000000, 10000000

MLFPInsertBench
par - 1,2,4,8,16,32,48,64
size - 1000000, 2000000, 5000000, 10000000

CLQInsertBench
par - 1,2,4,8,16,32
size - 1000000, 2000000, 5000000, 10000000

LTQInsertBench
par - 1,2,4,8,16,32
size - 1000000, 2000000, 5000000, 10000000


### reduce
15, drop first 5

SFLPReduceBench
par - 1,2,4,8,16,32,48,64
size - 1000000, 2000000, 5000000, 10000000

MLFPReduceBench
par - 1,2,4,8,16,32,48,64
size - 1000000, 2000000, 5000000, 10000000

LTQReduceBench
par - 1,2,4,8,16,32,48,64
size - 1000000, 2000000, 5000000, 10000000


### map
15, drop first 5

SFLPMapBench
par - 1,2,4,8,16,32,48,64
size - 1000000, 2000000, 5000000

MLFPMapBench
par - 1,2,4,8,16,32,48,64
size - 1000000, 2000000, 5000000

LTQMapBench
par - 1,2,4,8,16,32,48,64
size - 1000000, 2000000, 5000000


### histogram
15, drop first 5

SLFPHistBench
par - 1,2,4,8,16,32
size - 500000, 1000000

MLFPHistBench
par - 1,2,4,8,16,32
size - 500000, 1000000

LTQHistBench
par - 1,2,4,8,16,32
size - 500000, 1000000


### communication
4, drop first 1

SLFPCommBench
par - 1
size - 200000000

MLFPCommBench
par - 1,2,4,8,16,32,64
size - 200000000

SLFPCommBench
par - 1,2,4,8
size - 200000000

