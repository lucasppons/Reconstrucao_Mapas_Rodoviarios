import pickle

LINE_FLUSH = '\r\033[K'

# tracksetCount = len(os.listdir('data/tracksets'))
tracksetCount = 1

trackedPointCount = 0
untrackedPointCount = 0

for tracksetFileIndex in range(tracksetCount):
  print(f'{LINE_FLUSH}Counting points: {tracksetFileIndex+1}/{tracksetCount} sets', end='')

  with open(f'data/tracksets/trackset_{tracksetFileIndex}.pkl', 'rb') as tracksetFile:
    trackset = pickle.load(tracksetFile)

    for track in trackset:
      if len(track['points']) < 2:
        untrackedPointCount += len(track['points']) - track['split']
      else:
        trackedPointCount += len(track['points']) - track['split']

  tracksetFileIndex += 1

print(f'{LINE_FLUSH}Points counted! Tracked: {trackedPointCount}, Untracked: {untrackedPointCount}, Total: {trackedPointCount + untrackedPointCount}')
