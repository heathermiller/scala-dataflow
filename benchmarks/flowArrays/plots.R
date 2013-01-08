#! /usr/bin/R --vanilla -f

source('load_data.R')

library(ggplot2)
library(tikzDevice)

outh <- function(fn, p) {
  tikz(paste('plots/', fn, '.tex', sep = ''), width = 2.5, height = 3)
  print(p)
  dev.off()
}

outw <- function(fn, p) {
  tikz(paste('plots/', fn, '.tex', sep = ''), width = 5, height = 3)
  print(p)
  dev.off()
}

tickfct <- function(x) {
  c(30, 100, 250, 500, 1000, 2000, 3000, 4000, 6000, 8000)
  ## low <- ceiling(x[[1]])
  ## hi  <- floor(x[[2]])
  ## c(low, seq(low - low %% 1000, hi - hi %% 1000, by = 1000))
}

geoms <- function(p) {
  p <- p + geom_point(size = 2.5)
  #p <- p + geom_line()
  p <- p + geom_path()
  p
}

settheme <- function(p) {
  p <- p + theme_bw()
  p <- p + theme(legend.position = "none")
  p <- p + theme(axis.title.x = element_text(size=9),
                 axis.title.y = element_text(size=9),
                 axis.text.x  = element_text(size=7),
                 axis.text.y  = element_text(size=7))
  p
}

logyscale <- function(p, name) {
  p <- p + scale_y_log10(name = name,
                         breaks = tickfct)
  p + coord_cartesian(ylim = c(30,8000))
}

## Parallelization plots

p <- ggplot(mpardat, aes(x = factor(par),
                         y = time,
                         group = bench,
                         color = bench,
                         shape = bench))
p <- geoms(p)
p <- logyscale(p, name = "time [ms]") + xlab("parallelization level")
p <- p + theme(legend.position = "none")
p <- settheme(p)

outh('par-time',p)

p <- ggplot(mpardat, aes(x = factor(par),
                         y = gctime,
                         group = bench,
                         color = bench,
                         shape = bench))
p <- geoms(p)
p <- logyscale(p, name = "time [ms]") + xlab("parallelization level")
p <- settheme(p)

outh('par-gctime',p)

p <- ggplot(mpardat, aes(x = factor(par),
                         y = ntime,
                         group = bench,
                         color = bench,
                         shape = bench))
p <- geoms(p)
p <- p + ylab("time [ms]") + xlab("parallelization level")
p <- settheme(p)

outh('par-ntime',p)

## Size plots


p <- ggplot(mszedat, aes(x = factor(size),
                         y = time,
                         group = bench,
                         color = bench,
                         shape = bench))
p <- geoms(p)
p <- logyscale(p, name = "time [ms]") + xlab("vector size")
p <- settheme(p)

outw('size-time', p)

p <- ggplot(mszedat, aes(x = factor(size),
                         y = gctime,
                         group = bench,
                         color = bench,
                         shape = bench))
p <- geoms(p)
p <- logyscale(p, name = "time [ms]") + xlab("vector size")
p <- settheme(p)

outw('size-gctime', p)

p <- ggplot(mszedat, aes(x = factor(size),
                         y = ntime,
                         group = bench,
                         color = bench,
                         shape = bench))
p <- geoms(p)
p <- p + ylab("time [ms]") + xlab("vector size")
p <- settheme(p)
p <- p + coord_cartesian(ylim = c(-400, 700))

outw('size-ntime', p)



##   geom_point(size = 2.5) +
##   geom_line() +
##   scale_y_log10(name = "execution time",
##                 breaks = tickfct, limits = c(30, 8000), expand = c(0,0))

## ggplot(mszedat, aes(x = factor(size),
##                     y = gctime,
##                     group = bench,
##                     color = bench)
##        ) +
##   geom_point() +
##   geom_line() +
##   scale_y_log10(name = "garbage collection time",
##                 breaks = tickfct, limits = c(30, 8000), expand = c(0,0))

## ggplot(mszedat, aes(x = factor(size),
##                     y = ntime,
##                     group = bench,
##                     color = bench)
##        ) +
##   ylim(c(-350, 600)) +
##   geom_point() +
##   geom_line()
## 
