#! /bin/sh

CURV="v1"
NOW=$(date '+%Y-%m-%dT%H.%M.%S')
PARS=$(seq 1 8)
LENS=$(seq 100000 50000 1000000)
BENCHES="ConcurrentLinkedQueueBench FlowPoolBench FlowPoolExperiment"
N=100

java -version > "data/${NOW}_javav.txt" 2>&1

cd ..

for par in $PARS; do
    for l in $LENS; do
        for bench in $BENCHES; do
            bdat=$(sbt "bench -Dsize=$l -Dpar=$par scala.dataflow.$bench $N" | \
                grep '^scala.dataflow')
            echo "$CURV\t$bench\t$par\t$l\t$bdat"
        done
    done
done > "benchmarks/data/${NOW}_bench.log"
