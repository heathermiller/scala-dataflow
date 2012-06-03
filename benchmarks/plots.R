source('load_data.R')
source('panel_fcts.R')

library(lattice)
library(car)

# Setup lattice
font.settings <- list(
                      font = 2,
                      fontfamily = "serif")
my.theme <- list(
                 par.xlab.text = font.settings,
                 par.ylab.text = font.settings,
                 par.sub.text  = font.settings,
                 par.main.text = font.settings,
                 par.add.text  = font.settings,
                 par.axis.text = font.settings)

### Display CPU-scaling of Qs

xyplot(time ~ factor(par) | arch * btype,
       data = mdat,
       groups = imptype,
       subset = ave(size, btype, machine, FUN = max) == size & lanef == 1,
       scales = list(
         y = list(log = 10),
         relation = "free"
         ),
       pch = c(1,2,4,5),
       col = "black",
       type = "o",
       key = list(
         text = list(lab = levels(mdat$imptype)),
         points = list(pch = c(1,2,4,5))
         ),
       xlab = "Number of CPUs",
       ylab = "Execution Time [ms]",
       par.settings = my.theme
)

### Display Size-scaling of Qs
xyplot(time ~ size | arch * btype,
       data = mdat,
       groups = imptype,
       subset = ave(par, btype, machine, FUN = min) == par & lanef == 1 & btype != "Comm",
       scales = list(
         relation = "free"
         ),
       pch = c(1,2,4,5),
       col = "black",
       type = "o",
       key = list(
         text = list(lab = levels(mdat$imptype)),
         points = list(pch = c(1,2,4,5))
         ),
       xlab = "Number of Elements Inserted",
       ylab = "Execution Time [ms]",
       par.settings = my.theme
)


### Personal notepad :)

# Analyze lanefactor
lfacdat <- mdat[mdat$machine == "lampmac14" & substr(mdat$bench,1,4) == "MLFP",]
lfacdat$bench = factor(lfacdat$bench, exclude = NULL)

scatterplot(time ~ size | interaction(lanef),
            data = lfacdat, subset = bench == "MLFPHistBench" & par == 4)

# Only use lanef = 1



boxplot(time ~ interaction(bench, size, lanef), subset = par == 8)



attach(lfacdat)
boxplot(time ~ interaction(size,par,bench))
detach(lfacdat)                           

scatterplot(time ~ lanef | interaction(par,machine,bench,size), data = mdat, smooth = FALSE)

ionly <- mdat[mdat$btype == "Insert",]
ionly$bench <- factor(ionly$bench, exclude = NULL)

scatterplot(time ~ par | bench, data = ionly, subset = btype == "Insert" & machine == "wolf",log="x")



scatterplot(time ~ par | bench, data = mdat, subset = mdat$size == 5000000 & mdat$btype == bencht)
