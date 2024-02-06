suppressPackageStartupMessages({
  library(tidyverse)
  library(data.table)
})

LINE_FLUSH <- '\r\033[K'

cat('Getting bounding coordinates...')

dataset <- fread('data/original_dataset.csv')

minLat <- dataset |> select(latitude) |> min()
minLong <- dataset |> select(longitude) |> min()

cat(paste(LINE_FLUSH, 'Min: (', minLat, ', ', minLong, ')\n', sep=''))

maxLat <- dataset |> select(latitude) |> max()
maxLong <- dataset |> select(longitude) |> max()

cat(paste('Max: (', maxLat, ', ', maxLong, ')\n', sep=''))
