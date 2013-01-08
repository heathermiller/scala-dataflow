## Read log files
# Read file list
## files <- list.files(path = 'data', pattern = ' eos_2013-01-04T18.57.37_bench.log');

## # Initialize data frame
## draw <- data.frame()

## # Read all log files
## for (f in files) {
##   tmp <- read.csv(paste('data',f, sep="/"),
##                   sep="\t",
##                   header=FALSE,
##                   col.names = c(
##                     "machine",
##                     "version",
##                     "bench",
##                     "par",
##                     "size",
##                     "class",
##                     paste("x", 1:20, sep=".")))
##   draw <- rbind(draw, tmp)
##   rm(tmp)
## }

draw <- read.csv('data/eos_2013-01-04T18.57.37_bench.log',
                 sep="\t",
                 header=FALSE,
                 col.names = c(
                   "machine",
                   "version",
                   "bench",
                   "par",
                   "size",
                   "class",
                   c(paste("x",  1:20, sep="."),
                     paste("gc", 1:20, sep="."))
                   )
                 )

# Reshape to long format and drop first 5 measures
dat <- reshape(draw,
               varying = list(
                 paste("x", 6:20, sep="."),
                 paste("gc", 6:20, sep=".")
                 ),
               v.names = c("time", "gctime"),
               direction = "long",
               drop = c("class",paste(c("x", "gc"), rep(1:5, each = 2), sep=".")))





## Normalize time
dat$ntime <- dat$time - dat$gctime

## library(reshape)

## ## Reshape to even longer format
## ldat <- melt(dat,
##              measure.vars = c("time", "gctime", "ntime")
##              )

## lmdat <- aggregate(value ~ version + machine + bench + par + size + variable,
##                    data = ldat, FUN = median)
             

## Aggregate to medians
mdat <- aggregate(cbind(time, gctime, ntime) ~
                  version + machine + bench + par + size,
                  data = dat, FUN = median)

mpardat <- mdat[mdat$size == 10000000,]
mszedat <- mdat[mdat$par  == 4,]
