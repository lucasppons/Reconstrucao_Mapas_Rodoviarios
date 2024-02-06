suppressPackageStartupMessages({
  library(tidyverse)
  library(data.table)
  library(png)
})

LINE_FLUSH <- '\r\033[K'

cat('Plotting isolated map...')

vertices <- fread('data/isolated/vertices.txt', col.names=c('id', 'long', 'lat', 'alt', 'online'))

isolated_map <- ggplot() +
  coord_cartesian(xlim=c(-57.555943,-49.716972), ylim=c(-33.898562,-27.044123), expand=FALSE) +
  annotation_raster(readPNG('data/base_map.png'), -Inf, Inf, -Inf, Inf) +
  geom_path(data=vertices, linewidth=0.3, aes(x=long, y=lat, color=online)) +
  scale_color_gradient(limits=c(0,1), low='red', high='green', guide='none') +
  theme_void()

ggsave('data/maps/isolated_map.png', plot=isolated_map, width=1943, height=1940, unit="px")

cat(paste(LINE_FLUSH, 'Plotted isolated map!\n', sep=''))
