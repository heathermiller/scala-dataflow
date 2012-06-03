source('load_data.R')
source('panel_fcts.R')

library(car)

### Display CPU-scaling of Qs


xyplot(time ~ factor(par) | btype * machine,
       data = dat,
       groups = imptype,
       subset = ave(size, btype, machine, FUN = max) == size & lanef == 1,
       auto.key = TRUE,
       scales = list(y = list(log = 10)),
       panel = panel.superpose,
       panel.groups = panel.ci,
       xlab = "Number of CPUs",
       ylab = "Execution Time [ms]",
       type = "l")


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
