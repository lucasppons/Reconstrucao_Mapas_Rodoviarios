import csv
from datetime import datetime
import math
import os
import pickle

LINE_FLUSH = '\r\033[K'

os.makedirs('data/tracksets', exist_ok=True)

print(f'{LINE_FLUSH}Reading dataset...', end='')

with open('data/sorted_dataset.csv', 'r') as datasetFile:
  datasetPointCount = sum(1 for _ in datasetFile) - 1

with open('data/sorted_dataset.csv', 'r') as datasetFile:
  trackset = []

  datasetReader = csv.DictReader(datasetFile)

  track = None
  lastPoint = None
  minLat = minLong = math.inf
  maxLat = maxLong = -math.inf

  tracksetPointCount = 0
  totalPointCount = 0
  tracksetFileIndex = 0

  for entry in datasetReader:
    id = entry['id_object']

    point = {
      'lat': float(entry['latitude']),
      'long': float(entry['longitude']),
      'online': entry['online'] == '1',
      'date': datetime.fromisoformat(entry['date'])
    }

    if lastPoint is None:
      distance = 0
    else:
      distance = math.sqrt(math.pow(point['lat'] - lastPoint['lat'], 2) + math.pow(point['long'] - lastPoint['long'], 2))

    newTrack = (track is None) or (id != track['id']) or ((point['date'] - lastPoint['date']).total_seconds() > 600) or (distance > 0.2)

    returned = (minLat <= point['lat'] <= maxLat) and (minLong <= point['long'] <= maxLong)

    if newTrack or returned:
      if track is not None:
        trackset.append(track)

      if tracksetPointCount >= 100000:
        with open(f'data/tracksets/trackset_{tracksetFileIndex}.pkl', 'wb') as tracksetFile:
          pickle.dump(trackset, tracksetFile)

        tracksetPointCount = 0
        tracksetFileIndex += 1
        trackset = []

      track = {'id': id, 'split': returned and not newTrack, 'points': [point]}

      if returned and not newTrack:
        track['points'].insert(0, lastPoint)
        minLat = maxLat = lastPoint['lat']
        minLong = maxLong = lastPoint['long']
      else:
        minLat = minLong = math.inf
        maxLat = maxLong = -math.inf
    else :
      track['points'].append(point)

    minLat = min(minLat, point['lat'])
    minLong = min(minLong, point['long'])
    maxLat = max(maxLat, point['lat'])
    maxLong = max(maxLong, point['long'])

    lastPoint = point

    tracksetPointCount += 1
    totalPointCount += 1

    if (totalPointCount % 100000) == 0:
      progress = int((totalPointCount / datasetPointCount) * 100)
      print(f'{LINE_FLUSH}Building tracksets: {progress}%', end='')

trackset.append(track)

with open(f'data/tracksets/trackset_{tracksetFileIndex}.pkl', 'wb') as tracksetFile:
  pickle.dump(trackset, tracksetFile)

print(f'{LINE_FLUSH}Tracksets built!')
