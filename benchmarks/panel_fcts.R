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


update.myopts <- function(key.labels, left = TRUE) {
  update(trellis.last.object(),
         scales = list(
           relation = "free"
           ),
         pch = c(1,2,4,5),
         col = "black",
         type = "o",
         key = list(
           text = list(lab = key.labels),
           points = list(pch = c(1,2,4,5)[1:length(key.labels)]),
           fontfamily = "serif"
           ),
         ylab = "Execution Time [ms]",
         par.settings = my.theme,
         strip = !left,
         strip.left = left
         )
}
