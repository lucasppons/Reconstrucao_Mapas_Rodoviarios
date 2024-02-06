suppressPackageStartupMessages({
  library(tidyverse)
  library(png)
})

LINE_FLUSH <- '\r\033[K'

cat('Plotting tracks map...')

track_files = list.files('data/tracks', full.names=TRUE)

track_count = length(track_files)

tracks = NULL

for (i in seq.int(1, track_count, 1000)) {
  chunk <- suppressMessages(read_delim(
      track_files[i:min(c(i+999, track_count))],
      delim=' ',
      col_names=c('long', 'lat', 'date'),
      id='file'
    )) |>
    mutate(track = parse_number(file)) |>
    select(!file)

  if (is.null(tracks)) {
    tracks <- chunk
  } else {
    tracks <- bind_rows(tracks, chunk)
  }
}

tracks_map <- ggplot() +
  coord_cartesian(xlim=c(-57.555943,-49.716972), ylim=c(-33.898562,-27.044123), expand=FALSE) +
  annotation_raster(readPNG('data/base_map.png'), -Inf, Inf, -Inf, Inf) +
  geom_path(data=tracks, aes(x=long, y=lat, group=track), alpha=0.5, linewidth=0.1) +
  theme_void()

ggsave('data/maps/tracks_map.png', plot=tracks_map, width=1943, height=1940, unit="px")

cat(paste(LINE_FLUSH, 'Plotted tracks map!\n', sep=''))
