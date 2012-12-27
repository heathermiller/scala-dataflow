## Read log files
# Read file list
files <- list.files(path = 'data', pattern = '*.log')

# Initialize data frame
draw <- data.frame()

# Read all log files
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

# Reshape to long format and drop first 5 measures
dat <- reshape(draw,
               varying = list(paste("x", 6:20, sep=".")),
               v.names = "time",
               direction = "long",
               drop = c("class",paste("x", 1:5, sep=".")))

## Read logn files
for (n in 2:3) {
  ## Fetch file list
  files <- list.files(path = 'data', pattern = paste('*.log',n,sep=''))

  ## Initialize data frame
  draw <- data.frame()

  ## Read all log files
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
                      paste("x", 1:n, sep=".")))
    draw <- rbind(draw, tmp)
    rm(tmp)
  }

  ## Reshape to long format
  tmp2 <- reshape(draw,
                  varying = paste("x", 1:n, sep="."),
                  v.names = "time",
                  direction = "long",
                  drop = c("class"))

  ## Append to whole dataset
  rbind(dat,tmp2)
  rm(tmp2)
}


## Classify benchmarks

# Benchmark Types
btypes = list(
  Insert = "Insert",
  Hist = "Histogram",
  Reduce = "Reduce",
  Comm = "Comm",
  Map = "Map")
dat$btype = ""
for (n in names(btypes))
  dat[grep(n, dat$bench, fixed = TRUE),"btype"] = btypes[[n]]
dat$btype = factor(dat$btype)

# Implementation Types
imptypes = list(
  CLQ = "ConcurrentLinkedQueue",
  SLFP = "Single-Lane FlowPool",
  MLFP = "Multi-Lane FlowPool",
  LTQ = "LinkedTransferQueue")
dat$imptype = ""
for (n in names(imptypes))
  dat[grep(n, dat$bench, fixed = TRUE),"imptype"] = imptypes[[n]]
dat$imptype = factor(dat$imptype)

# Add architecture
archs = list(
  wolf = "32-core Xeon",
  maglite = "UltraSPARC T2",
  lampmac14 = "4-core i7"
  )
dat$arch = ""
for (n in names(archs))
  dat[dat$machine == n,"arch"] = archs[[n]]
dat$arch = factor(dat$arch)

## Aggregate to medians
attach(dat)
mdat <- aggregate(time,
                  list(version = version,
                       machine = machine,
                       arch = arch,
                       bench = bench,
                       par = par,
                       lanef = lanef,
                       size = size,
                       btype = btype,
                       imptype = imptype),
                  median)
mdat$time <- mdat$x
detach(dat)

mdat$x <- NULL
