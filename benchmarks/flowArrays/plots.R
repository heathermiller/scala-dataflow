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
  tikz(paste('plots/', fn, '.tex', sep = ''), width = 5, height = 2.5)
  print(p)
  dev.off()
}

tickfct <- function(x) {
  c(30, 100, 250, 500, 1000, 2000, 3000, 4500, 6000, 8000)
  ## low <- ceiling(x[[1]])
  ## hi  <- floor(x[[2]])
  ## c(low, seq(low - low %% 1000, hi - hi %% 1000, by = 1000))
}

geoms <- function(p) {
  p <- p + geom_point(size = 2.5)
  p <- p + geom_line()
  p
}

settheme <- function(p, lpos) {
  p <- p + theme_bw()
  p <- p + theme(legend.position = lpos)
  p <- p + theme(axis.title.x = element_text(size=9),
                 axis.title.y = element_text(size=9),
                 axis.text.x  = element_text(size=7),
                 axis.text.y  = element_text(size=7),
                 legend.text  = element_text(size=8),
                 legend.background = element_rect(fill="transparent", color = NA))
  g <- guide_legend(title = NULL, keywidth = .8, keyheight = .8)
  p <- p + guides(color = g, shape = g)
  p
}

logyscale <- function(p, name) {
  p <- p + scale_y_log10(name = name,
                         breaks = tickfct)
  p + coord_cartesian(ylim = c(30,8000))
}

benchscale <- function(p) {
  labs <- c("FlowSeq","FS (zipMap)","FS (zipMapFold)","ParArray")
  p <- p + scale_color_discrete(labels = labs)
  p <- p + scale_shape_discrete(labels = labs)
  p
}

## Parallelization plots

p <- ggplot(mpardat, aes(x = factor(par),
                         y = time,
                         group = bench,
                         color = bench,
                         shape = bench))
p <- geoms(p)
p <- logyscale(p, name = "time [ms]") + xlab("parallelization level")
p <- benchscale(p)
p <- settheme(p, lpos = c(0.33, 0.15))

outh('par-time',p)

p <- ggplot(mpardat, aes(x = factor(par),
                         y = gctime,
                         group = bench,
                         color = bench,
                         shape = bench))
p <- geoms(p)
p <- logyscale(p, name = "time [ms]") + xlab("parallelization level")
p <- benchscale(p)
p <- settheme(p, lpos = c(0.33, 0.4))

outh('par-gctime',p)

p <- ggplot(mpardat, aes(x = factor(par),
                         y = ntime,
                         group = bench,
                         color = bench,
                         shape = bench))
p <- geoms(p)
p <- p + ylab("time [ms]") + xlab("parallelization level")
p <- benchscale(p)
p <- settheme(p, lpos = c(0.33, 0.15))

outh('par-ntime',p)

## Size plots


p <- ggplot(mszedat, aes(x = factor(size),
                         y = time,
                         group = bench,
                         color = bench,
                         shape = bench))
p <- geoms(p)
p <- logyscale(p, name = "time [ms]") + xlab("vector size")
p <- benchscale(p)
p <- settheme(p, lpos = c(0.14, 0.2))

outw('size-time', p)

p <- ggplot(mszedat, aes(x = factor(size),
                         y = gctime,
                         group = bench,
                         color = bench,
                         shape = bench))
p <- geoms(p)
p <- logyscale(p, name = "time [ms]") + xlab("vector size")
p <- benchscale(p)
p <- settheme(p, lpos = c(0.14, 0.45))

outw('size-gctime', p)

p <- ggplot(mszedat, aes(x = factor(size),
                         y = ntime,
                         group = bench,
                         color = bench,
                         shape = bench))
p <- geoms(p)
p <- p + ylab("time [ms]") + xlab("vector size")
p <- benchscale(p)
p <- p + coord_cartesian(ylim = c(-400, 700))
p <- settheme(p, lpos = c(0.87, 0.2))

outw('size-ntime', p)
