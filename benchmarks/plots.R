#! /usr/bin/R --vanilla -f

library(car)

d <- read.csv('bencht.log',sep="\t",header=FALSE)
dl <- reshape(d, varying = list(4:103), v.names="conc", direction = "long")

dl$par   <- dl$V1
dl$size  <- dl$V2
dl$bench <- dl$V3

agg <- aggregate(dl$conc, list(size = dl$size, par = dl$par, bench = dl$bench), median)

col <- palette()[1:5]
pc  <- c(rep(1, 5),rep(2, 5))

scatterplot(x ~ size | interaction(par,bench), data=agg, col = rep(col,2), pch = pc,
            legend.title = "# Threads & Benchmark", ylab = "execution time", xlab = "size")
#scatterplot(x ~ size | interaction(par,bench), data=agg, col = rep(col,2), pch = pc, log = "y")

col <- palette()[1:3]
pc  <- c(rep(1, 3),rep(2, 3))

scatterplot(x ~ par | interaction(size,bench), data = agg, col = rep(col,2), pch = pc,
            legend.title = "Size & Benchmark", ylab = "execution time", xlab = "size")

## n <- length(levels(agg$par))
## colors <- rainbow(n)

## xrange <- range(agg$size)
## yrange <- range(agg$x)

## plot(xrange, yrange, type="n", xlab="Number of insertions", ylab="Execution Time")
## j <- 0
## for (b in levels(agg$bench)) {
##   j <- j + 1
##   i <- 0
##   for (p in levels(agg$par)) {
##     i <- i+1
##     sset <- agg[agg$par == p & agg$bench == b,] 
##     lines(sset$size, sset$x, type="b", lwd=1, lty = j,
##           col = colors[i], pch = 20 + j)
##   }
## }
