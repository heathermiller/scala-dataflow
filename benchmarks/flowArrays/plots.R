#! /usr/bin/R --vanilla -f

source('load_data.R')

library(lattice)

xyplot(time ~ factor(par),
       groups =  bench,
       dat = mdat,
       subset = size == 10000000,
       auto.key = T)

xyplot(time ~ factor(size),
       groups =  bench,
       dat = mdat,
       subset = par == 4,
       auto.key = T)
