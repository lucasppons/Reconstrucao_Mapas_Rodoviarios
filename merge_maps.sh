if [ $# -eq 0 ] ; then
  echo "Pass index of folder to merge"

  exit 1
fi

track_insertion/map_merging_script.sh $1
