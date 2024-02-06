track_insertion/final_merge_script.sh

if [ $? -eq 0 ] ; then
  Rscript plot_rebuilt_map.R
else
  exit 1
fi
