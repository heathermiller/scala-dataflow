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
insertCls    = c("CLQInsertBench", "FPInsertBench", "FPSealedInsertBench", "LTQInsertBench")
histogramCls = c("FPHistBench", "FPUnsafeHistBench", "LTQHistBench")
reduceCls    = c("FPReduceBench", "LTQReduceBench")

dat$btype = factor(c("Insert","Histogram","Reduce"))

dat[dat$bench %in% insertCls   ,]$btype = "Insert"
dat[dat$bench %in% histogramCls,]$btype = "Histogram"
dat[dat$bench %in% reduceCls   ,]$btype = "Reduce"

attach(dat)
mdat <- aggregate(time,
                  list(version = version,
                       machine = machine,
                       bench = bench,
                       par = par,
                       size = size,
                       btype = btype),
                  median)
mdat$time <- mdat$x
detach(dat)
