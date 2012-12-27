#! /usr/bin/R --vanilla -f

source('load_data.R')
source('panel_fcts.R')

library(lattice)

### Display hist only diag
## Not yet finished. TODO: legend (& input size normalization?)
xyplot(time ~ factor(par), groups = interaction(arch,imptype),
       subset = ave(size, btype, machine, FUN = max) == size & lanef == 1 & btype == "Histogram",
       data = mdat,
       pch = rep(c(1,2,4,5), 3),
       lty = rep(c(1,2,3), c(3,3,3)),
       type = "o",
       col = "black",
       scale = list(y = list(log = 10)),
       legend = list(
         top = list(
           fun = draw.key,
           x = -10,
           y = 0,
           args = list(key = list(
                         points = list(col = 1:2),
                         text = list("foo","bar"))
             )
           ),
         bottom = list(
           fun = draw.key,
           x = -10,
           y = 0,
           args = list(key = list(
                         points = list(col = 1:2),
                         text = list("foo","bar"))
             )
           )
         )
       )


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
update.myopts(key.labels = levels(mdat$imptype),3,3)
dev.off()

# par == 8 & lanef == 1
xyplot(time ~ size | arch * btype,
       data = mdat,
       groups = imptype,
       subset = par == 8 & lanef == 1 & btype != "Comm" & btype != "Histogram",
       xlab = "Size of Benchmark",
)

pdf("graphs/size-scaling-par8.pdf", height = 5)
update.myopts(key.labels = levels(mdat$imptype),3,3)
dev.off()

### Display lane-factor-scaling of Qs
# par == 8 & size == max
xyplot(time ~ factor(lanef) | btype,
       data = mdat,
       group = arch,
       subset = par == 8 &
                (ave(size, btype, machine, FUN = max) == size |
                 btype == "Comm" & size == 100000000) &
                machine != "wolf" & imptype == "Multi-Lane FlowPool",
       xlab = "Number of Lanes per Inserting Thread"
       )

pdf("graphs/lanef-scaling.pdf", height = 5)
update.myopts(key.labels = levels(mdat$arch)[1:2], matrix = FALSE)
dev.off()
