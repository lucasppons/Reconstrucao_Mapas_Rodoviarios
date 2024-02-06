if [ $# -ne 2 ] ; then
  echo "Pass index of trackset to rebuild and output index"

  exit 1
fi

track_insertion/script_to_run.sh $1 $2
