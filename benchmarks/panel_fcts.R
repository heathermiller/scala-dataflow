### Setup lattice settings
font.settings <- list(fontfamily = "serif")

my.theme <- list(
                 par.xlab.text = font.settings,
                 par.ylab.text = font.settings,
                 par.sub.text  = font.settings,
                 par.main.text = font.settings,
                 par.add.text  = font.settings,
                 par.axis.text = font.settings,
                 add.text      = font.settings,
                 strip.background = list(col = c('white')),
                 strip.text    = font.settings
                 )

panel.ci <- function(x, y, ...)
{
  x <- as.numeric(x)
  yagg <- aggregate(y,by = list(ox = x),quantile)
  
  yl <- yagg$x[,2]
  ym <- yagg$x[,3]
  yu <- yagg$x[,4]

  ox <- yagg$ox

  panel.arrows(ox, yl, ox, yu, col = 'black',
               length = 0.07, unit = "native",
               lwd = 0.7,
               angle = 90, code = 3)

  panel.xyplot(ox, ym, ...)
}


update.myopts <- function(key.labels,h=0,w=0,matrix=TRUE) {

  # Setup graphical parameters
  pars <- my.theme
  if (matrix) {
    pars$layout.heights <- list(strip = rep(c(0, 1), c(h-1, 1)))
    pars$layout.widths  <- list(strip.left = rep(c(1, 0), c(1, w-1)))
  }

  # Setup strip fcts
  top.pf <- function(which.given, ...) {
    if (which.given == 1 & panel.number() > (h-1)*w)
      strip.default(which.given = 1, ...)
  }

  left.pf <- function(which.given, which.panel, ...) {
    if (which.given == 2 & panel.number() %% w == 1)
      strip.default(which.given = 1, which.panel[2], ...)
  }

  # Update chart
  update(trellis.last.object(),
         scales = list(
           relation = "free"
           ),
         pch = c(1,2,4,5),
         col = "black",
         type = "o",
         ## key = list(
         ##   text = list(lab = key.labels),
         ##   points = list(pch = c(1,2,4,5)[1:length(key.labels)]),
         ##   fontfamily = "serif"
         ##   ),
         ylab = "Execution Time [ms]",
         par.settings = pars,
         strip = if (matrix) top.pf else TRUE,
         strip.left = if (matrix) left.pf else FALSE
         )
}



