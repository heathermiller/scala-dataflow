#! /bin/sh

PARS=`seq 1 10`
LENS="10000 100000 500000"
BENCHES="ConcurrentLinkedQueueBench FlowPoolBench"
N=100

java -version 2>&1 > javav.log

cd ..

for par in $PARS; do
    for l in $LENS; do
        for bench in $BENCHES; do
            bdat=$(sbt "bench -Dsize=$l -Dpar=$par scala.dataflow.$bench 100" | \
                grep '^scala.dataflow')
            echo "$par $l $bdat"
        done
    done
done > benchmarks/bench.log
