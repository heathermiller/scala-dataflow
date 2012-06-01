# Read file list
files <- list.files(path = 'data', pattern = '*.log')

# Initialize data frame
draw <- data.frame()

# Read all files
for (f in files) {
  tmp <- read.csv(paste('data',f, sep="/"),
                  sep="\t",
                  header=FALSE,
                  col.names = c(
                    "machine",
                    "version",
                    "bench",
                    "par",
                    "lanef",
                    "size",
                    "class",
                    paste("x", 1:20, sep=".")))
  draw <- rbind(draw, tmp)
  rm(tmp)
}

# Reshape to long format
dat <- reshape(draw,
               varying = list(paste("x", 1:20, sep=".")),
               v.names = "time",
               direction = "long",
               drop = "class")

# Classify benchmarks
insertCls = c(
  "CLQInsertBench", "SLFPInsertBench", 
  "LTQInsertBench", "MLFPInsertBench")
histogramCls = c(
  "MLFPHistBench", "SLFPHistBench", "LTQHistBench")
reduceCls = c(
  "SLFPReduceBench", "MLFPReduceBench", "LTQReduceBench")
commCls = c(
  "SLFPCommBench", "MLFPCommBench", "LTQCommBench")
mapCls = c(
  "SLFPMapBench", "MLFPMapBench", "LTQMapBench")

dat$btype = ""

dat[dat$bench %in% insertCls   ,"btype"] = "Insert"
dat[dat$bench %in% histogramCls,"btype"] = "Histogram"
dat[dat$bench %in% reduceCls   ,"btype"] = "Reduce"
dat[dat$bench %in% commCls     ,"btype"] = "Comm"
dat[dat$bench %in% mapCls      ,"btype"] = "Map"

dat$btype = factor(dat$btype)

attach(dat)
mdat <- aggregate(time,
                  list(version = version,
                       machine = machine,
                       bench = bench,
                       par = par,
                       lanef = lanef,
                       size = size,
                       btype = btype),
                  median)
mdat$time <- mdat$x
detach(dat)

mdat$x <- NULL
