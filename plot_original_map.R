suppressPackageStartupMessages({
  library(tidyverse)
  library(data.table)
  library(png)
})

LINE_FLUSH <- '\r\033[K'

cat('Plotting original map...')

dataset <- fread('data/sorted_dataset.csv') |>
  mutate(online = as.integer(online == 1))

original_map <- ggplot() +
  coord_cartesian(xlim=c(-57.555943,-49.716972), ylim=c(-33.898562,-27.044123), expand=FALSE) +
  annotation_raster(readPNG('data/base_map.png'), -Inf, Inf, -Inf, Inf) +
  geom_point(data=dataset, aes(x=longitude, y=latitude, color=online), size=0.1, alpha=0.5) +
  scale_color_gradient(limits=c(0,1), low='red', high='green', guide='none') +
  theme_void()

ggsave('data/maps/original_map.png', plot=original_map, width=1943, height=1940, unit="px")

cat(paste(LINE_FLUSH, 'Plotted original map!\n', sep=''))
