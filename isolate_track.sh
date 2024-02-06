track_insertion/track_isolation_script.sh

if [ $? -eq 0 ] ; then
  Rscript plot_isolated_map.R
else
  exit 1
fi
