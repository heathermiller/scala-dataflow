#! /usr/bin/env perl

use strict;
use warnings;

use POSIX qw(strftime);

## Configuration

my $version = "v3";
my $N = 20;

my $def_lanes = [ 1.5, 2, 3, 4 ];
my $def_par   = [ 1, 2, 4, 8 ];
my $def_b_par = [ 1, 2, 4, 8, 16, 32, 48, 64 ];

my $gconf = {
    wolf => {
        SLFPInsertBench => { par => $def_par,   size => [1000000,10000000,15000000], lanes => [1] },
        CLQInsertBench  => { par => $def_par,   size => [1000000,10000000,15000000], lanes => [1] },
        LTQInsertBench  => { par => $def_par,   size => [1000000,10000000,15000000], lanes => [1] },
        MLFPInsertBench => { par => $def_b_par, size => [1000000,10000000,15000000], lanes => $def_lanes }
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
