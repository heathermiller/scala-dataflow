#! /usr/bin/R --vanilla -f

source('load_data.R')
source('panel_fcts.R')

library(lattice)


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
update.myopts(key.labels = levels(mdat$imptype),5,3)
dev.off()


### Display Size-scaling of Qs
# par == 1 & lanef == 1
xyplot(time ~ size | arch * btype,
       data = mdat,
       groups = imptype,
       subset = par == 1 & lanef == 1 & btype != "Comm" & btype != "Histogram",
       xlab = "Size of Benchmark")

pdf("graphs/size-scaling-par1.pdf", height = 5)
update.myopts(key.labels = levels(mdat$imptype),4,3)
dev.off()

# par == 8 & lanef == 1
xyplot(time ~ size | arch * btype,
       data = mdat,
       groups = imptype,
       subset = par == 8 & lanef == 1 & btype != "Comm" & btype != "Histogram",
       xlab = "Size of Benchmark",
)

pdf("graphs/size-scaling-par8.pdf", height = 5)
update.myopts(key.labels = levels(mdat$imptype),4,3)
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

pdf("graphs/lanef-scaling.pdf", height = 5)
update.myopts(key.labels = levels(mdat$arch)[1:2], matrix = FALSE)
dev.off()
