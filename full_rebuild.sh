rm -rf data/rebuilt/
rm -rf data/merged/
rm -rf data/final/

COUNT=$(($(ls -l data/tracksets/ | wc -l) - 1))

SPLIT_THREADS=$(($(nproc) * 3 / 4))

if [ $COUNT -lt  $SPLIT_THREADS ] ; then
  SPLIT_THREADS=$COUNT
fi

SPLIT_COUNT=$((COUNT / SPLIT_THREADS))

PIDS=""

for i in $(seq 0 $((SPLIT_THREADS - 1))) ; do
  START_INDEX=$((i * SPLIT_COUNT))
  END_INDEX=$((START_INDEX + SPLIT_COUNT - 1))

  if [ $i -eq $((SPLIT_THREADS - 1)) ] ; then
    END_INDEX=$(($COUNT - 1))
  fi

  ./partial_rebuild.sh $START_INDEX $END_INDEX $i &

  PIDS="$PIDS $!"
done

trap "kill -2 $PIDS ; killall java ; killall javac ; exit 1" SIGINT

wait

./final_merge.sh
