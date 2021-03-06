#! /usr/bin/env perl

use strict;
use warnings;

use POSIX qw(strftime);

## Configuration

my $version = "v2";
my $N = 20;

my $eos_def_par  = [ 4 ];
my $eos_pars     = [ 1, 2, 4 ];
my $eos_def_size = [ 10000000 ];
my $eos_sizes    = [ 8000000, 9000000, 10000000, 11000000, 12000000, 13000000 ];

my $chara_def_par  = [ 8 ];
my $chara_pars     = [ 1, 2, 4, 8 ];
my $chara_def_size = [ 5000000 ];
my $chara_sizes    = [ 2000000, 3000000, 4000000, 5000000, 6000000, 7000000 ];

my $chara_fa_def_bench = [ { par => $chara_def_par, size => $chara_sizes },
			   { par => $chara_pars,    size => $chara_def_size } ];

my $eos_fa_def_bench = [ { par => $eos_def_par, size => $eos_sizes },
			   { par => $eos_pars,    size => $eos_def_size } ];
    
my $gconf = {
    chara => {
        FAScalProdBench    => $chara_fa_def_bench,
        FAScalProdBenchZM  => $chara_fa_def_bench,
        FAScalProdBenchZMF => $chara_fa_def_bench,
        PAScalProdBench    => [ { par => $chara_def_par, size => $chara_sizes }]
    },
    eos =>  {
        FAScalProdBench    => $eos_fa_def_bench,
        FAScalProdBenchZM  => $eos_fa_def_bench,
        FAScalProdBenchZMF => $eos_fa_def_bench,
        PAScalProdBench    => [ { par => $eos_def_par, size => $eos_sizes }]
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

    my @gctimes = (0.0) x $N;
    my $benchi = 0;

    if ($arglen > 0) {
      $sbtcommand = $ARGV[0];
    }
    open(BENCH,
         $sbtcommand . " 'bench -Dsize=$size -Dpar=$par scala.dataflow.array.bench.$bench $N' |");
    while (<BENCH>) {
        $gctimes[$benchi] += $1 * 1000 if (/^\[GC \[PSYoungGen: [^]]+\] [^]]+, ([0-9.]+) secs]/);
        $benchi++ if (/^\[Full GC \(System\) /);
        
        if (/^scala.dataflow.array.bench/) {
            chomp;
            $" = "\t";
            print LOG "$host\t$version\t$bench\t$par\t$size\t";
            print LOG $_;
            print LOG "\t@gctimes\n";
            print "$host\t$version\t$bench\t$par\t$size\t";
            print $_;
            print "\t@gctimes\n";
            $benchi = 0;
            @gctimes = (0.0) x $N;
        }
    }
    close(BENCH);
}

# Loop over benchmarks
for my $bench (keys %$c) {
    my @bc = @{$c->{$bench}};
    for my $cfg (@bc) {
	for my $size (@{$cfg->{size}}) {
	    for my $par (@{$cfg->{par}}) {
		&exec_bench($bench,$par,$size);
	    }
	}
    }
}

close(LOG);
