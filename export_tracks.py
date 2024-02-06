import math
import os
import pickle

LINE_FLUSH = '\r\033[K'

os.makedirs('data/tracks', exist_ok=True)

tracksetCount = len(os.listdir('data/tracksets'))

trackFileIndex = 0

trackCountDigitsEstimate = 0

for tracksetFileIndex in range(tracksetCount):
  os.makedirs(f'data/tracks/{tracksetFileIndex}', exist_ok=True)

  print(f'{LINE_FLUSH}Exporting tracks: {tracksetFileIndex+1}/{tracksetCount} sets', end='')

  with open(f'data/tracksets/trackset_{tracksetFileIndex}.pkl', 'rb') as tracksetFile:
    trackset = pickle.load(tracksetFile)

    if trackCountDigitsEstimate == 0:
      trackCountDigitsEstimate = math.ceil(math.log10(len(trackset) * tracksetCount))

    for track in trackset:
      if len(track['points']) < 2:
        continue

      with open(f'data/tracks/{tracksetFileIndex}/track_{str(trackFileIndex).rjust(trackCountDigitsEstimate, "0")}.txt', 'w') as trackFile:
        for point in track['points']:
          trackFile.write(f'{point["long"]} {point["lat"]} {point["date"].timestamp()} {point["online"]}\n')

      trackFileIndex += 1

  tracksetFileIndex += 1

print(f'{LINE_FLUSH}Tracks exported!')
