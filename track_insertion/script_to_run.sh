if [ $# -ne 2 ] ; then
  echo "Pass index of trackset to rebuild and output index"

  exit 1
fi

#To Compile:
CODE_PATH="track_insertion/" #path to the MapConstruction folder.
cd $CODE_PATH
make -s

if [ $? -ne 0 ] ; then
  exit 1
fi

#To Run:
INPUT_PATH="../data/tracks/$1/" #path to the folder that constains all input tracks
OUTPUT_PATH="../data/rebuilt/$2/$1/" #path to the folder where output will be written
EPS=0.01 #epsilon
HAS_ALTITUDE=false #if input file has altitude information
ALT_EPS=4.0 #minimum altitude difference between two streets

mkdir -p $OUTPUT_PATH

java -Djava.util.logging.config.file=src/logging.properties -cp bin/ mapconstruction2.MapConstruction $INPUT_PATH $OUTPUT_PATH $EPS $HAS_ALTITUDE $ALT_EPS
