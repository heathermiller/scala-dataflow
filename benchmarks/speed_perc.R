source('load_data.R')

tmp <- mdat[mdat$lanef == 1 & mdat$par == 8,]

wdat <- reshape(tmp, v.names = "time",
                idvar = c("version", "machine", "arch", "par", "size", "btype"),
                timevar = "imptype",
                drop = c("bench", "lanef"),
                direction = "wide")

wdat$fp <- wdat[,"time.Multi-Lane FlowPool"]

wdat$q  <- pmin(wdat[,"time.ConcurrentLinkedQueue"],
                wdat[,"time.LinkedTransferQueue"],
                na.rm = TRUE)

wdat$rat <- wdat$fp / wdat$q

fastest <- min(wdat$rat, na.rm = TRUE)
slowest <- max(wdat$rat, na.rm = TRUE)

100 - fastest * 100
100 - slowest * 100

wdat[!is.na(wdat$hirat) & wdat$hirat == fastest,]
wdat[!is.na(wdat$lorat) & wdat$lorat == slowest,]
