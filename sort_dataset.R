suppressPackageStartupMessages({
  library(tidyverse)
  library(data.table)
})

LINE_FLUSH <- '\r\033[K'

cat('Sorting dataset...')

fread('data/original_dataset.csv') |>
  arrange(id_object, date) |>
  fwrite('data/sorted_dataset.csv')

cat(paste(LINE_FLUSH, 'Sorted dataset!\n', sep=''))
