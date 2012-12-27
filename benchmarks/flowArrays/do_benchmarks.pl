#! /usr/bin/env perl

use strict;
use warnings;

use POSIX qw(strftime);

## Configuration

my $version = "v1";
my $N = 20;

my $def_par       = [ 1, 2, 4, 8 ];
my $def_eos_size  = [ 6000000, 12000000, 24000000 ];
my $def_chara_size  = [ 5000000, 10000000, 15000000, 20000000, 25000000, 30000000 ];

my $gconf = {
    chara => {
        FAScalProdBench    => { par => $def_par,   size => $def_chara_size },
        FAScalProdBenchZM  => { par => $def_par,   size => $def_chara_size },
        FAScalProdBenchZMF => { par => $def_par,   size => $def_chara_size },
        PAScalProdBench    => { par => $def_par,   size => $def_chara_size }
    }
    eos =>  {
        FAScalProdBench    => { par => $def_par,   size => $def_eos_size },
        FAScalProdBenchZM  => { par => $def_par,   size => $def_eos_size },
        FAScalProdBenchZMF => { par => $def_par,   size => $def_eos_size },
        PAScalProdBench    => { par => $def_par,   size => $def_eos_size }
    }
};

## Script

chdir('../..');

# Get our hostname
my $host = `hostname -s`; chomp $host;

# Create fname
my $dstr = strftime('%Y-%m-%dT%H.%M.%S',localtime);
my $bfname = "benchmarks/flowArrays/data/${host}_${dstr}_";
my $lfname = "${bfname}bench.log";
my $vfname = "${bfname}javav.txt";

`java -version > $vfname 2>&1`;

die 'Machine not found' unless (defined $gconf->{$host});

open(LOG, ">> $lfname");

# Fetch our configuration
my $c = $gconf->{$host};

sub exec_bench {
    my ($bench,$par,$size) = @_;
    my $sbtcommand = "sbt";
    my $arglen = @ARGV;
    if ($arglen > 0) {
      $sbtcommand = $ARGV[0];
    }
    open(BENCH,
         $sbtcommand . " 'bench -Dsize=$size -Dpar=$par scala.dataflow.array.bench.$bench $N' |");
    while (<BENCH>) {
        if (/^scala.dataflow.array.bench/) {
            print LOG "$host\t$version\t$bench\t$par\t$size\t";
            print LOG $_;
            print "$host\t$version\t$bench\t$par\t$size\t";
	    print $_;
        }
    }
    close(BENCH);
}

# Loop over benchmarks
for my $bench (keys %$c) {
    my $bc = $c->{$bench};
    for my $size (@{$bc->{size}}) {
        for my $par (@{$bc->{par}}) {
            &exec_bench($bench,$par,$size);
        }
    }
}

close(LOG);
