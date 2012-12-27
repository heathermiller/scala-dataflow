source('load_data.R')

get.extime <- function(tmp) {
  fastest <- min(tmp$rat, na.rm = TRUE)
  slowest <- max(tmp$rat, na.rm = TRUE)

  rbind(tmp[!is.na(tmp$rat) & tmp$rat == fastest,c("arch","par","size","btype","rat","dec")],
        tmp[!is.na(tmp$rat) & tmp$rat == slowest,c("arch","par","size","btype","rat","dec")])
}

# Insert, 1 lane data only
tmp <- mdat[mdat$lanef == 1 & mdat$btype == "Insert",]

# Get min
attach(tmp)
tmp$min <- ave(time, version, machine, arch, bench, lanef, size, btype, imptype, FUN = min)
detach(tmp)

# Get min rows
tmp[tmp$min == tmp$time,]

# Reshape
wdat <- reshape(tmp[tmp$min == tmp$time,], v.names = c("min","par"),
                idvar = c("version", "machine", "arch", "size"),
                timevar = "imptype",
                drop = c("bench", "lanef","btype","time"),
                direction = "wide")



# Mins per type
wdat$fp    <- wdat[,"min.Multi-Lane FlowPool"]
wdat$fppar <- wdat[,"par.Multi-Lane FlowPool"]

wdat$q     <- pmin(wdat[,"min.ConcurrentLinkedQueue"],
                   wdat[,"min.LinkedTransferQueue"],
                   na.rm = TRUE)
wdat$qpar  <- apply(wdat, 1, function(x) { 
  if(x["min.ConcurrentLinkedQueue"] == x["q"])
    x["par.ConcurrentLinkedQueue"]
  else x["min.LinkedTransferQueue"] }
)

# Rate
wdat$rat <- wdat$fp / wdat$q
wdat$dec <- (1 - wdat$rat) * 100

# Order and output
wdat[order(wdat$machine,wdat$size),
     c("version", "machine", "arch", "size", "fp", "fppar", "q", "qpar", "rat", "dec")]
