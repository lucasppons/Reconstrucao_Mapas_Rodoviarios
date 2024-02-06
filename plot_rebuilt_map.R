suppressPackageStartupMessages({
  library(tidyverse)
  library(data.table)
  library(png)
})

LINE_FLUSH <- '\r\033[K'

cat('Plotting rebuilt map...')

vertices <- fread('data/final/vertices.txt', col.names=c('id', 'long', 'lat', 'alt', 'online'))

edges <- fread('data/final/edges.txt', col.names=c('id', 'v1', 'v2')) |>
  left_join(vertices, by=c("v1" = "id")) |>
  rename(v1.long = long, v1.lat = lat, v1.online = online) |>
  left_join(vertices, by=c("v2" = "id")) |>
  rename(v2.long = long, v2.lat = lat, v2.online = online) |>
  mutate(online = (v1.online + v2.online) / 2) |>
  select(!contains('alt') & !contains('.online'))

rebuilt_map <- ggplot() +
  coord_cartesian(xlim=c(-57.555943,-49.716972), ylim=c(-33.898562,-27.044123), expand=FALSE) +
  annotation_raster(readPNG('data/base_map.png'), -Inf, Inf, -Inf, Inf) +
  geom_segment(data=edges, linewidth=0.3, aes(x=v1.long, y=v1.lat, xend=v2.long, yend=v2.lat, color=online)) +
  scale_color_gradient(limits=c(0,1), low='red', high='green', guide='none') +
  theme_void()

ggsave('data/maps/rebuilt_map.png', plot=rebuilt_map, width=1943, height=1940, unit="px")

cat(paste(LINE_FLUSH, 'Plotted rebuilt map!\n', sep=''))
