if [ $# -ne 3 ] ; then
  echo "Pass range of tracksets to rebuild and output index"

  exit 1
fi

for i in $(seq $1 $2) ; do
  echo "Rebuilding $i"

  ./rebuild_map.sh $i $3

  if [ $? -ne 0 ] ; then
    exit 1
  fi
done

./merge_maps.sh $3
