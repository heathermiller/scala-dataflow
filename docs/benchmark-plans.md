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

SFLPReduceBench
par - 1,2,4,8
size - 1000000, 5000000, 10000000

MFLPReduceBench
par - 1,2,4,8,16,32,48,64
size - 1000000, 5000000, 10000000

LTQReduceBench
par - 1,2,4,8


### map


### histogram


### continuous insert




## maglite


