#! /usr/bin/env perl 

use strict;
use warnings;

use File::Basename;

my $vers = "v4";
my $fn = $ARGV[0];
my $cnt = $ARGV[1];
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
    if (/^(?<cls>scala.dataflow.bench.\w+\$)(?<x>
             (?:\s+\d+){$cnt}
           )$/x) {
        my $x = $+{'x'};
        my $cls = $+{'cls'};
        $x =~ s/\s+/\t/g;
        print "$machine\t$vers\t$bm\t$par\t1\t$size\t$cls$x\n";
    }
}

close(F);
