#! /usr/bin/R --vanilla -f

source('load_data.R')

library(lattice)

pdf("pres_graphs/cpu-scaling-insert.pdf", width = 9, height = 5);
xyplot(time ~ factor(par) | arch,
       data = mdat,
       groups = imptype,
       subset = ave(size, btype, machine, FUN = max) == size &
                lanef == 1 & btype == "Insert",
       scales = list(y = list(log = 10), relation = "free"),
       xlab = "Number of CPUs",
       ylab = "Time",
       type = "o",
       layout = c(3,1)
       )
dev.off()

pdf("pres_graphs/cpu-scaling-reduce.pdf", width = 9, height = 5);
xyplot(time ~ factor(par) | arch,
       data = mdat,
       groups = imptype,
       subset = ave(size, btype, machine, FUN = max) == size &
                lanef == 1 & btype == "Reduce",
       scales = list(y = list(log = 10), relation = "free"),
       xlab = "Number of CPUs",
       ylab = "Time",
       type = "o",
       layout = c(3,1)
       )
dev.off()

xyplot(time ~ size,
       data = mdat,
       groups = imptype,
       subset = lanef == 1 & btype == "Comm",
       xlab = "Size of Benchmark")

update.myopts <- function(key.labels,h=0,w=0,matrix=TRUE) {

  # Setup graphical parameters
  pars <- my.theme
  if (matrix) {
    pars$layout.heights <- list(strip = rep(c(0, 1), c(h-1, 1)))
    pars$layout.widths  <- list(strip.left = rep(c(1, 0), c(1, w-1)))
  }

  # Setup strip fcts
  top.pf <- function(which.given, ...) {
    if (which.given == 1 & panel.number() > (h-1)*w)
      strip.default(which.given = 1, ...)
  }

  left.pf <- function(which.given, which.panel, ...) {
    if (which.given == 2 & panel.number() %% w == 1)
      strip.default(which.given = 1, which.panel[2], ...)
  }

  # Update chart
  update(trellis.last.object(),
         scales = list(
           relation = "free"
           ),
         pch = c(1,2,4,5),
         col = "black",
         type = "o",
         ## key = list(
         ##   text = list(lab = key.labels),
         ##   points = list(pch = c(1,2,4,5)[1:length(key.labels)]),
         ##   fontfamily = "serif"
         ##   ),
         ylab = "Execution Time [ms]",
         par.settings = pars,
         strip = if (matrix) top.pf else TRUE,
         strip.left = if (matrix) left.pf else FALSE
         )
}




