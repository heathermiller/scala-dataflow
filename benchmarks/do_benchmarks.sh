#! /bin/sh

CURV="v2"
NOW=$(date '+%Y-%m-%dT%H.%M.%S')
PARS=$(seq 1 1)
LENS=$(seq 100000 10000 500000)
MACHINE="chara"
#BENCHES="FPInsertBench CLQInsertBench LTQInsertBench FPReduceBench FPSealedInsertBench FPUnsafeHistBench FPHistBenchg LTQHistBench LTQReduceBench"
BENCHES="FPHistBench"
N=20

java -version > "data/${NOW}_javav.txt" 2>&1

cd ..

for par in $PARS; do
    for l in $LENS; do
        for bench in $BENCHES; do
            bdat=$(sbt "bench -Dsize=$l -Dpar=$par scala.dataflow.bench.$bench $N" | \
                grep '^scala.dataflow.bench')
            echo "$MACHINE\t$CURV\t$bench\t$par\t$l\t$bdat"
        done
    done
done > "benchmarks/data/${NOW}_bench.log"
