draw <- read.csv('data/2012-05-09T00.53.24_bench.log',sep="\t",header=FALSE,
                 col.names = c("version", "bench", "par", "size", "class",
                   paste("x", 1:100, sep=".")))
dat <- reshape(draw, varying = list(paste("x", 1:100, sep=".")),
               v.names = "time", direction = "long", drop = "class")


attach(dat)
mdat <- aggregate(time, list(version = version, bench = bench, par = par, size = size),
                  median)
detach(dat)
