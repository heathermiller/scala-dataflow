#! /usr/bin/R --vanilla -f

source('load_data.R')
source('panel_fcts.R')

library(lattice)

### Setup lattice settings
font.settings <- list(fontfamily = "serif")

my.theme <- list(
                 par.xlab.text = font.settings,
                 par.ylab.text = font.settings,
                 par.sub.text  = font.settings,
                 par.main.text = font.settings,
                 par.add.text  = font.settings,
                 par.axis.text = font.settings,
                 add.text      = font.settings,
                 strip.background = list(col = c('white')),
                 strip.text    = font.settings)

## top.pf <- function(which.given, ...) {
##   if (which.given == 1 & panel.number() >= 13)
##     strip.default(which.given = 1, ...)
## }

## left.pf <- function(which.given, ...) {
##   if (which.given == 2 & panel.number() %% 3 == 2)
##     strip.default(which.given = 2, ...)
## }

### Display CPU-scaling of Qs
# size == max & lanef == 1


xyplot(time ~ factor(par) | arch * btype,
       data = mdat,
       groups = imptype,
       subset = ave(size, btype, machine, FUN = max) == size & lanef == 1,
       scales = list(y = list(log = 10)),
       xlab = "Number of CPUs"
       )
pdf("graphs/cpu-scaling.pdf", height = 10)
update.myopts(key.labels = levels(mdat$imptype))
dev.off()


### Display Size-scaling of Qs
# par == 1 & lanef == 1
xyplot(time ~ size | arch * btype,
       data = mdat,
       groups = imptype,
       subset = par == 1 & lanef == 1 & btype != "Comm",
       xlab = "Size of Benchmark")
pdf("graphs/size-scaling-par1.pdf", height = 10)
update.myopts(key.labels = levels(mdat$imptype))
dev.off()

# par == 8 & lanef == 1
xyplot(time ~ size | arch * btype,
       data = mdat,
       groups = imptype,
       subset = par == 8 & lanef == 1 & btype != "Comm",
       xlab = "Size of Benchmark",
)
pdf("graphs/size-scaling-par8.pdf", height = 10)
update.myopts(key.labels = levels(mdat$imptype))
dev.off()

### Display lane-factor-scaling of Qs
# par == 8 & size == max
xyplot(time ~ factor(lanef) | btype,
       data = mdat,
       group = arch,
       subset = par == 8 & ave(size, btype, machine, FUN = max) == size &
                machine != "wolf" & imptype == "Multi-Lane FlowPool",
       xlab = "Factor of Lanes"
       )
pdf("graphs/lanef-scaling.pdf", height = 10)
update.myopts(key.labels = levels(mdat$arch)[1:2], left = FALSE)
dev.off()
