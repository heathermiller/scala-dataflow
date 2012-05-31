#! /usr/bin/env perl

use strict;
use warnings;

use POSIX qw(strftime);

## Configuration

my $version = "v3";
my $N = 20;

my $def_lanes = [ 1, 1.5, 2, 3, 4 ];
my $def_par   = [ 1, 2, 4, 8 ];
my $def_b_par = [ 1, 2, 4, 8, 16, 32, 64 ];

my $gconf = {
    wolf => {
	SLFPInsertBench => { par => [1,2,4,8],       size => [2000000,5000000,15000000],            lanes => [1] },
	MLFPInsertBench => { par => $def_b_par,      size => [2000000,5000000,15000000],            lanes => [1] },
	LTQInsertBench  => { par => [1,2,4,8],       size => [2000000,5000000,15000000],            lanes => [1] },
	CLQInsertBench  => { par => [1,2,4,8],       size => [2000000,5000000,15000000],            lanes => [1] },
	SLFPReduceBench => { par => [1,2,4,8],       size => [2000000,5000000,10000000],            lanes => [1] },
	MLFPReduceBench => { par => $def_b_par,      size => [2000000,5000000,10000000],            lanes => [1] },
	LTQReduceBench  => { par => [1,2,4,8],       size => [2000000,5000000,10000000],            lanes => [1] },
	SLFPMapBench    => { par => [1,2,4,8],       size => [2000000,5000000,10000000],            lanes => [1] },
	MLFPMapBench    => { par => $def_b_par,      size => [2000000,5000000,10000000],            lanes => [1] },
	LTQMapBench     => { par => [1,2,4,8],       size => [2000000,5000000,10000000],            lanes => [1] },
	SLFPHistBench   => { par => [1,2,4,8,16],    size => [500000,1000000],            lanes => [1] },
	MLFPHistBench   => { par => [1,2,4,8,16,32], size => [500000,1000000],            lanes => [1] },
	LTQHistBench    => { par => [1,2,4,8,16],    size => [500000,1000000],            lanes => [1] },
	SLFPCommBench   => { par => [1],             size => [50000000],                  lanes => [1] },
	MLFPCommBench   => { par => $def_b_par,      size => [50000000],                  lanes => [1] },
	LTQCommBench    => { par => [1,2,4],         size => [50000000],                  lanes => [1] },
    },
    maglite => {
	SLFPInsertBench => { par => [1,2,4,8,16],    size => [1000000,2000000,5000000,15000000],            lanes => $def_lanes },
	MLFPInsertBench => { par => $def_b_par,      size => [1000000,2000000,5000000,15000000],            lanes => $def_lanes },
	LTQInsertBench  => { par => [1,2,4,8,16,32], size => [1000000,2000000,5000000,15000000],            lanes => $def_lanes },
	CLQInsertBench  => { par => [1,2,4,8,16,32], size => [1000000,2000000,5000000,15000000],            lanes => $def_lanes },
	SLFPReduceBench => { par => $def_b_par,      size => [1000000,2000000,5000000,10000000],            lanes => $def_lanes },
	MLFPReduceBench => { par => $def_b_par,      size => [1000000,2000000,5000000,10000000],            lanes => $def_lanes },
	LTQReduceBench  => { par => $def_b_par,      size => [1000000,2000000,5000000,10000000],            lanes => $def_lanes },
	SLFPMapBench    => { par => $def_b_par,      size => [1000000,2000000,5000000],            lanes => $def_lanes },
	MLFPMapBench    => { par => $def_b_par,      size => [1000000,2000000,5000000],            lanes => $def_lanes },
	LTQMapBench     => { par => $def_b_par,      size => [1000000,2000000,5000000],            lanes => $def_lanes },
	SLFPHistBench   => { par => [1,2,4,8,16,32], size => [500000,1000000],            lanes => $def_lanes },
	MLFPHistBench   => { par => [1,2,4,8,16,32], size => [500000,1000000],            lanes => $def_lanes },
	LTQHistBench    => { par => [1,2,4,8,16,32], size => [500000,1000000],            lanes => $def_lanes },
	SLFPCommBench   => { par => [1],             size => [100000000],                  lanes => $def_lanes },
	MLFPCommBench   => { par => $def_b_par,      size => [100000000],                  lanes => $def_lanes },
	LTQCommBench    => { par => [1,2,4],         size => [100000000],                  lanes => $def_lanes },
    },
    lampmac14 => {
	SLFPInsertBench => { par => $def_par,        size => [2000000,5000000,15000000],            lanes => $def_lanes },
	MLFPInsertBench => { par => $def_par,        size => [2000000,5000000,15000000],            lanes => $def_lanes },
	LTQInsertBench  => { par => $def_par,        size => [2000000,5000000,15000000],            lanes => $def_lanes },
	CLQInsertBench  => { par => $def_par,        size => [2000000,5000000,15000000],            lanes => $def_lanes },
	SLFPReduceBench => { par => $def_par,        size => [2000000,5000000,10000000],            lanes => $def_lanes },
	MLFPReduceBench => { par => $def_par,        size => [2000000,5000000,10000000],            lanes => $def_lanes },
	LTQReduceBench  => { par => $def_par,        size => [2000000,5000000,10000000],            lanes => $def_lanes },
	SLFPMapBench    => { par => $def_par,        size => [2000000,5000000,10000000],            lanes => $def_lanes },
	MLFPMapBench    => { par => $def_par,        size => [2000000,5000000,10000000],            lanes => $def_lanes },
	LTQMapBench     => { par => $def_par,        size => [2000000,5000000,10000000],            lanes => $def_lanes },
	SLFPHistBench   => { par => $def_par,        size => [1500000,4000000],            lanes => $def_lanes },
	MLFPHistBench   => { par => $def_par,        size => [1500000,4000000],            lanes => $def_lanes },
	LTQHistBench    => { par => $def_par,        size => [1500000,4000000],            lanes => $def_lanes },
	SLFPCommBench   => { par => $def_par,        size => [100000000],                  lanes => $def_lanes },
	MLFPCommBench   => { par => $def_par,        size => [100000000],                  lanes => $def_lanes },
	LTQCommBench    => { par => $def_par,        size => [100000000],                  lanes => $def_lanes },
    },
    eos =>  {
        SLFPInsertBench => { par => $def_par,   size => [1,2,3], lanes => [1] },
        CLQInsertBench  => { par => $def_par,   size => [1,2,3], lanes => [1] },
        LTQInsertBench  => { par => $def_par,   size => [1,2,3], lanes => [1] },
        MLFPInsertBench => { par => $def_b_par, size => [1,2,3], lanes => $def_lanes }
    }
};

## Script

chdir('..');

# Get our hostname
my $host = `hostname`; chomp $host;

# Create fname
my $dstr = strftime('%Y-%m-%dT%H.%M.%S',localtime);
my $bfname = "benchmarks/data/${host}_${dstr}_";
my $lfname = "${bfname}bench.log";
my $vfname = "${bfname}javav.txt";

`java -version > $vfname 2>&1`;

die 'Machine not found' unless (defined $gconf->{$host});

open(LOG, ">> $lfname");

# Fetch our configuration
my $c = $gconf->{$host};

sub exec_bench {
    my ($bench,$par,$size,$lanef) = @_;
    my $lanes = int($par*$lanef);
    open(BENCH,
         "sbt 'bench -Dsize=$size -Dpar=$par -Dlanes=$lanes scala.dataflow.bench.$bench $N' |");
    while (<BENCH>) {
        if (/^scala.dataflow.bench/) {
            print LOG "$host\t$version\t$bench\t$par\t$lanef\t$size\t";
            print LOG $_;
        }
    }
    close(BENCH);
}

# Loop over benchmarks
for my $bench (keys %$c) {
    my $bc = $c->{$bench};
    for my $par (@{$bc->{par}}) {
        for my $size (@{$bc->{size}}) {
            for my $lanef (@{$bc->{lanes}}) {
                &exec_bench($bench,$par,$size,$lanef);
            }
        }
    }
}

close(LOG);
