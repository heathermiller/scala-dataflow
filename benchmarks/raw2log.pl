#! /usr/bin/env perl 

use strict;
use warnings;

use File::Basename;

my $vers = "v4";
my $fn = $ARGV[0];
my $bm;
my $par;
my $size;

basename($fn) =~ /^(\w+)_/;
my $machine = $1;

open(F, "<", $fn);

while(<F>) {
    if (/^ > bench -Dpar=(?<par>\d+) -Dsize=(?<size>\d+)/) {
        $par  = $+{'par'};
        $size = $+{'size'};
    }
    if (/^scala.dataflow.bench.(?<bench>\w+) \d+$/) {
        $bm = $+{'bench'};
    }
    if (/^(?<cls>scala.dataflow.bench.\w+\$) \s+
          (?<x1>\d+) \s+
          (?<x2>\d+)$/x) {
        print "$machine\t$vers\t$bm\t$par\t1\t$size\t$+{cls}\t$+{x1}\t$+{x2}\n";
    }
}

close(F);
