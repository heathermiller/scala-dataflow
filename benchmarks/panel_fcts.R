panel.ci <- function(x, y, col.line = 'black', ...)
{
  x <- as.numeric(x)
  yagg <- aggregate(y,by = list(ox = x),quantile)
  
  yl <- yagg$x[,2]
  ym <- yagg$x[,3]
  yu <- yagg$x[,4]

  ox <- yagg$ox

  panel.arrows(ox, yl, ox, yu, col = col.line,
               length = 0.07, unit = "native",
               lwd = 0.7,
               angle = 90, code = 3)

  panel.xyplot(ox, ym, col.line = col.line, ...)
}
