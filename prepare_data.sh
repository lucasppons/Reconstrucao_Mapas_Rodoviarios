if [ ! -f data/sorted_dataset.csv ]; then
  Rscript sort_dataset.R
fi

rm -rf data/tracksets/
python3 build_tracksets.py

rm -rf data/tracks/
python3 export_tracks.py
