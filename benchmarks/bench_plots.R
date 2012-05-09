#! /usr/bin/evn R --vanilla -f

source('load_data.R')

pdf("graphs/single-thread.pdf",width=10)
scatterplot(x ~ size | bench, data = mdat[mdat$par == 1,],
            legend.title = "Queue Type",
            xlab = "# inserted elements", ylab = "time [s]")
title("Single threaded insertions")
dev.off()

pdf("graphs/multi-thread.pdf",width=10)
tmp <- mdat[mdat$size == 1000000 & mdat$bench != "FlowPoolExperiment",]
tmp$bench <- factor(tmp$bench, exclude = NULL)
scatterplot(x ~ par | bench, data = tmp,
            legend.title = "Queue Type",
            xlab = "# threads", ylab = "time [s]")
title("1'000'000 insertions")
dev.off()
