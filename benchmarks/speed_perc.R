source('load_data.R')

get.extime <- function(tmp) {
  fastest <- min(tmp$rat, na.rm = TRUE)
  slowest <- max(tmp$rat, na.rm = TRUE)

  rbind(tmp[!is.na(tmp$rat) & tmp$rat == fastest,c("arch","par","size","btype","rat","dec")],
        tmp[!is.na(tmp$rat) & tmp$rat == slowest,c("arch","par","size","btype","rat","dec")])
}

wdat <- reshape(mdat[mdat$lanef == 1,], v.names = "time",
                idvar = c("version", "machine", "arch", "par", "size", "btype"),
                timevar = "imptype",
                drop = c("bench", "lanef"),
                direction = "wide")

wdat$fp <- wdat[,"time.Multi-Lane FlowPool"]

wdat$q  <- pmin(wdat[,"time.ConcurrentLinkedQueue"],
                wdat[,"time.LinkedTransferQueue"],
                na.rm = TRUE)

wdat$rat <- wdat$fp / wdat$q
wdat$dec <- (1 - wdat$rat) * 100

## Global execution time evaluation
get.extime(wdat[wdat$par == 8,])

## Insert execution time
get.extime(wdat[wdat$btype == "Insert",])

## Histogram execution time
get.extime(wdat[wdat$btype == "Histogram" & wdat$par >= 8,])

## Reduce execution time
get.extime(wdat[wdat$btype == "Reduce",])

## Map execution time
get.extime(wdat[wdat$btype == "Map",])
