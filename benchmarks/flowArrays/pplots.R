#! /usr/bin/R --vanilla -f

source('load_data.R')

library(ggplot2)
library(tikzDevice)

outh <- function(fn, p, w) {
  tikz(paste('plots/', fn, '.tex', sep = ''), width = w, height = 3)
  print(p)
  dev.off()
}

outw <- function(fn, p, h) {
  tikz(paste('plots/', fn, '.tex', sep = ''), width = 5, height = h)
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
  p <- p + geom_point(size = 1.8)
  p <- p + geom_line()
  p
}

settheme <- function(p, lpos) {
  p <- p + theme_bw()
  p <- p + theme(legend.position = lpos)
  p <- p + theme(axis.title.x = element_text(size=8),
                 axis.title.y = element_text(size=8),
                 axis.text.x  = element_text(size=6),
                 axis.text.y  = element_text(size=6),
                 plot.title   = element_text(size=8),
                 legend.text  = element_text(size=7),
                 legend.background = element_rect(fill="transparent", color = NA),
                 panel.background  = element_rect(fill="transparent", color = NA),
                 plot.background   = element_rect(fill="transparent", color = NA))
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
  labs <- c("FlowArray","FA (zipMap)","FA (zipMapFold)","ParArray")
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
p <- p + ggtitle("Execution Time")
p <- logyscale(p, name = "time [ms]") +
  scale_x_discrete(name = "parallelization level",
                   expand = c(.07,0))
p <- benchscale(p)
p <- settheme(p, lpos = c(0.45, 0.15))

outh('pres-par-time', p, w = 1.8)

p <- ggplot(mpardat, aes(x = factor(par),
                         y = gctime,
                         group = bench,
                         color = bench,
                         shape = bench))
p <- geoms(p)
p <- p + ggtitle("GC Time")
p <- logyscale(p, name = "time [ms]") +
     scale_x_discrete(name = "parallelization level",
                      expand = c(.07,0))
p <- benchscale(p)
p <- settheme(p, lpos = c(0.5, 0.4))
p <- p + theme(axis.title.y = element_blank(),
               axis.text.y  = element_blank(),
               legend.position = "none")


outh('pres-par-gctime', p, w = 1.6)

p <- ggplot(mpardat, aes(x = factor(par),
                         y = ntime,
                         group = bench,
                         color = bench,
                         shape = bench))

p <- p + geom_point(size = 1.8)
p <- p + geom_line(linetype = "dotted")
p <- p + ggtitle("Normalized Time")
p <- p + ylab("time [ms]") +
  scale_x_discrete(name = "parallelization level",
                   expand = c(.07,0))
p <- benchscale(p)
p <- settheme(p, lpos = c(0.5, 0.15))
p <- p + theme(axis.title.y = element_blank(),
               legend.position = "none")

outh('pres-par-ntime',p , w = 1.8)

## Size plots

p <- ggplot(mszedat, aes(x = size,
                         y = time,
                         group = bench,
                         color = bench,
                         shape = bench))
p <- geoms(p)
p <- p + ggtitle("Execution Time")
p <- logyscale(p, name = "time [ms]") +
    scale_x_continuous(name = "vector size",
                     breaks = c(8e6,1.3e7))
p <- benchscale(p)
p <- settheme(p, lpos = c(0.45, 0.15))

outh('pres-size-time', p, w = 1.8)

p <- ggplot(mszedat, aes(x = size,
                         y = gctime,
                         group = bench,
                         color = bench,
                         shape = bench))
p <- geoms(p)
p <- p + ggtitle("GC Time")
p <- logyscale(p, name = "time [ms]") +
  scale_x_continuous(name = "vector size",
                     breaks = c(8e6,1.3e7))
p <- benchscale(p)
p <- settheme(p, lpos = c(0.14, 0.45))
p <- p + theme(axis.title.y = element_blank(),
               axis.text.y  = element_blank(),
               legend.position = "none")

outh('pres-size-gctime', p, w = 1.6)

p <- ggplot(mszedat[mszedat$bench != "PAScalProdBench",],
            aes(x = size,
                y = ntime,
                group = bench,
                color = bench,
                shape = bench))
p <- p + geom_point(size = 1.8)
p <- p + geom_line(linetype = "dotted")
p <- p + ggtitle("Normalized Time")
p <- p + ylab("time [ms]") + 
  scale_x_continuous(name = "vector size",
                     breaks = c(8e6,1.3e7))
p <- benchscale(p)
p <- p + coord_cartesian(ylim = c(-400, 700))
p <- settheme(p, lpos = c(0.87, 0.2))
p <- p + theme(axis.title.y  = element_blank(),
               legend.position = "none")

outh('pres-size-ntime', p, w = 1.8)

