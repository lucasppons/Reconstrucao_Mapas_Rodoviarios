package mapconstruction2;

/**
 * Frechet-based map construction 2.0 Copyright 2013 Mahmuda Ahmed and Carola Wenk
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *  ------------------------------------------------------------------------
 *
 *  This software is based on the following article. Please cite this article when using this code
 * as part of a research publication:
 *
 *  Mahmuda Ahmed and Carola Wenk, "Constructing Street Networks from GPS Trajectories", European
 * Symposium on Algorithms (ESA): 60-71, Ljubljana, Slovenia, 2012
 *
 *  ------------------------------------------------------------------------
 *
 * Author: Mahmuda Ahmed Filename: MapMerging.java
 *
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * An object that represents a track.
 *
 */
class CurveFile {
  String fileName;
  ArrayList<Vertex> curve;
  double minX;
  double minY;
  double maxX;
  double maxY;

  CurveFile() {
    this.fileName = "";
    this.curve = new ArrayList<Vertex>();

    minX = minY = Double.POSITIVE_INFINITY;
    maxX = maxY = Double.NEGATIVE_INFINITY;
  }

  CurveFile(String curveName, ArrayList<Vertex> curve) {
    this.fileName = curveName;
    this.curve = curve;

    minX = minY = Double.POSITIVE_INFINITY;
    maxX = maxY = Double.NEGATIVE_INFINITY;
  }

  public String getFileName() {
    return fileName;
  }

  public ArrayList<Vertex> getPose() {
    return curve;
  }

  public double getLength() {
    double length = 0;
    for (int i = 1; i < curve.size(); i++) {
      length = length + curve.get(i - 1).dist(curve.get(i));
    }
    return length;
  }

  public boolean enclosesVertex(Vertex v) {
    return (v.getX() >= minX) && (v.getX() <= maxX) && (v.getY() >= minY) && (v.getY() <= maxY);
  }

  public static CurveFile readTrack(List<Vertex> track) {
    CurveFile poseFile = new CurveFile();
    poseFile.fileName = "";

    for (int i = 0; i < track.size(); i++) {
      Vertex point = track.get(i);

      poseFile.curve.add(point);

      poseFile.minX = Math.min(poseFile.minX, point.getX());
      poseFile.minY = Math.min(poseFile.minY, point.getY());
      poseFile.maxX = Math.max(poseFile.maxX, point.getX());
      poseFile.maxY = Math.max(poseFile.maxY, point.getY());
    }

    poseFile.minX -= 0.1;
    poseFile.minY -= 0.1;
    poseFile.maxX += 0.1;
    poseFile.maxY += 0.1;

    return poseFile;
  }
}

/**
 * An object that takes a set of poses as input, construct graph and write two
 * files one for vertices and one for edges.
 */
public class MapMerging {

  public static int curveid; // counter for pose
  public static String curveName; // file name for the pose

  private static final Logger logger = Logger.getAnonymousLogger();

  private static final String LINE_FLUSH = "\r\033[K";

  /**
   * Writes the constructed map into files.
   */

  public static void writeToFile(List<Vertex> vList, String fileName) {

    try {
      int count = 0;
      BufferedWriter bwedges = new BufferedWriter(new FileWriter(fileName
          + "/edges.txt"));
      BufferedWriter bvertex = new BufferedWriter(new FileWriter(fileName
          + "/vertices.txt"));


      for (int i = 0; i < vList.size(); i++) {
        Vertex v = vList.get(i);
        bvertex.write(i + "," + v.getX() + "," + v.getY() +","+ v.getZ() + "," + v.getOnlineAvg() + "\n");

        for (int j = 0; j < v.getDegree(); j++) {

          if (i != v.getAdjacentElementAt(j)) {

            bwedges.write(count + "," + i + ","
                + v.getAdjacentElementAt(j) + "\n");

            count++;
          }
        }
      }


      bwedges.close();
      bvertex.close();
    } catch (Exception ex) {
      System.out.println(ex.toString());
    }
  }

  public static void readFromFile(List<Vertex> constructedMap, Map<String, Integer> map, String fileName) {
    try {
      Scanner vertexScanner = new Scanner(new File(fileName + "/vertices.txt")).useDelimiter("[\\s,]");
      Scanner edgeScanner = new Scanner(new File(fileName + "/edges.txt")).useDelimiter("[\\s,]");

      int from = -1;
      int to = -1;

      while (vertexScanner.hasNext()) {
        int id = vertexScanner.nextInt();
        double x = vertexScanner.nextDouble();
        double y = vertexScanner.nextDouble();
        double z = vertexScanner.nextDouble();
        double online = vertexScanner.nextDouble();

        Vertex vertex = new Vertex(x, y, z);

        vertex.addEntry(online);

        constructedMap.add(vertex);

        map.put(vertex.toString(), constructedMap.indexOf(vertex));

        while (true) {
          if (from == id) {
            vertex.addElementAdjList(to);
          }

          if (!edgeScanner.hasNext()) break;

          int edgeId = edgeScanner.nextInt();
          from = edgeScanner.nextInt();
          to = edgeScanner.nextInt();

          if (from != id) break;
        }
      }


      vertexScanner.close();
      edgeScanner.close();
    } catch (Exception ex) {
      System.out.println(ex.toString());
    }
  }

  /**
   * Computes interval on edge e for a line segment consists of
   * (currentIndex-1)-th and currentIndex-th vertices of pose and return true
   * if edge e has a part of white interval else false.
   */

  public boolean isWhiteInterval(Edge edge, List<Vertex> pose,
      int currentIndex, double eps, double altEps) {
    Line line = new Line(pose.get(currentIndex - 1), pose.get(currentIndex));

    if (Math.abs(line.avgAltitude() - edge.getLine().avgAltitude()) <= altEps) {
      return line.pIntersection(edge, eps);
    } else {
      return false;
    }
  }

  /**
   * Sets corresponding interval endpoints on Edge.
   */
  public void setEndPointsOnEdge(Edge edge, int startIndex, int endIndex,
      double cstart, double vstart) {
    edge.setCurveStartIndex(startIndex);
    edge.setCurveStart(startIndex + cstart);
    edge.setEdgeStart(vstart);

    edge.setCurveEnd(endIndex - 1 + edge.getCurveEnd());
    edge.setCurveEndIndex(endIndex);
  }

  /**
   * Scans for next white interval on an Edge starting from index newstart of
   * pose.
   */
  public void computeNextInterval(Edge edge, List<Vertex> pose, int newstart,
      double eps, double altEps) {

    // Compute next white interval on edge.
    boolean first = true;
    boolean debug = false;

    int startIndex = 0;
    double cstart = 0, vstart = 0;

    if (newstart >= pose.size()) {
      edge.setCurveEndIndex(pose.size());
      edge.setDone(true);
      return;
    }

    for (int i = newstart; i < pose.size(); i++) {
      boolean result = isWhiteInterval(edge, pose, i, eps, altEps);

      // first = true means we are still looking for our first interval
      // starting from newstart.
      // !result indicate Line(pose.get(i), pose.get(i+1)) doesn't contain
      // white interval.
      // we can just ignore if(first && !result).

      if (first && result) {
        // first segment on the white interval
        first = false;
        startIndex = i - 1;
        cstart = edge.getCurveStart();
        vstart = edge.getEdgeStart();

        // if the white interval ends within the same segment
        if (edge.getCurveEnd() < 1) {
          this.setEndPointsOnEdge(edge, startIndex, i, cstart, vstart);
          return;
        }
      } else if (!first && result) {
        // not the first segment on the white interval
        if (edge.getCurveEnd() < 1) {
          // if the white interval ends within that segment
          this.setEndPointsOnEdge(edge, startIndex, i, cstart, vstart);
          return;
        }
      } else if (!first && !result) {
        // the white interval ends at 1.0 of previous segment
        this.setEndPointsOnEdge(edge, startIndex, i, cstart, vstart);
        return;
      }
    }

    if (first) {
      // if the last segment on the curve is the first segment of that
      // interval
      edge.setCurveEndIndex(pose.size());
      edge.setDone(true);
    } else {
      edge.setCurveStartIndex(startIndex);
      edge.setCurveStart(startIndex + cstart);
      edge.setEdgeStart(vstart);

      edge.setCurveEnd(pose.size() - 2 + edge.getCurveEnd());
      edge.setCurveEndIndex(pose.size() - 2);
    }

    return;
  }

  /**
   * Updates constructedMap by adding an Edge. Detail description of the
   * algorithm is in the publication.
   */
  public void updateMap(List<Vertex> constructedMap,
      Map<String, Integer> map, Edge edge) {

    // update the map by adding a new edge
    Vertex v;
    int parent = -1;
    int child = -1;

    String keyParent = edge.getVertex1().toString();
    String keyChild = edge.getVertex2().toString();
    // find the index of parent node
    if (map.containsKey(keyParent)) {
      parent = map.get(keyParent).intValue();

      constructedMap.get(parent).addEntry(edge.getVertex1());
    } else {
      v = edge.getVertex1();
      constructedMap.add(v);
      parent = constructedMap.indexOf(v);
      map.put(keyParent, parent);
    }
    // find the index of child node
    if (map.containsKey(keyChild)) {
      child = map.get(keyChild).intValue();

      constructedMap.get(child).addEntry(edge.getVertex2());
    } else {
      v = edge.getVertex2();
      constructedMap.add(v);
      child = constructedMap.indexOf(v);
      map.put(keyChild, child);
    }
    // update the map
    if (parent == -1 || child == -1) {
      logger.log(Level.SEVERE, "inconsistent graph child, parent :"
          + child + ", " + parent);
    } else if (parent != child) {

      constructedMap.get(parent).addElementAdjList(child);
      constructedMap.get(child).addElementAdjList(parent);

      logger.log(Level.FINEST, "child, parent :" + child + ", " + parent);
      logger.log(Level.FINEST, "child, parent :" + parent + ", " + child);

    }
  }

  /**
   * Adds a split point on an Edge.
   *
   * @param newVertexPosition
   *            represents position of a new Vertex
   */
  public void edgeSplit(List<Vertex> constructedMap,
      Map<String, Integer> map, Edge edge, double newVertexPosition) {

    Vertex v1 = edge.getVertex1();
    Vertex v2 = edge.getVertex2();

    String key1 = v1.toString();
    String key2 = v2.toString();

    // call of this method always after updateMap which ensures
    // map.containsKey(key1) is
    // always true.
    int index1 = map.get(key1).intValue();
    int index2 = map.get(key2).intValue();

    Vertex v = edge.getLine().getVertex(newVertexPosition);

    // splitting an edge on split point vertex v

    String key = v.toString();

    int index = map.get(key).intValue();

    if (index == index1 || index == index2) {
      return;
    }

    logger.log(Level.FINER, "Index = " + index1 + " " + index2 + " "
        + index);

    edge.addSplit(newVertexPosition, index);
  }

  /**
   * Commits edge splitting listed in List<Integer> Edge.edgeSplitVertices.
   */

  public void commitEdgeSplits(List<Edge> edges, Map<String, Integer> map,
      List<Vertex> graph) {
    for (int e = 0; e < edges.size(); e++) {
      Edge edge = edges.get(e);

      for (int i = 0; i < edge.getEdgeSplitPositions().size(); i++) {
        double newPosition = 1 - edge.getEdgeSplitPositions()
            .get(i).doubleValue();
        edge.addSplit(newPosition,
            edge.getEdgeSplitVertices().get(i));
      }

      List<Integer> edgeVertexSplits = edge.getEdgeSplitVertices();
      int splitSize = edgeVertexSplits.size();

      if (splitSize == 0) {
        return;
      }

      Vertex v1 = edge.getVertex1();
      Vertex v2 = edge.getVertex2();

      String key1 = v1.toString();
      String key2 = v2.toString();

      int index1 = map.get(key1).intValue();
      int index2 = map.get(key2).intValue();

      boolean updateV1 = false, updateV2 = false;

      logger.log(Level.FINER, "commitEdgeSplits " + splitSize);

      for (int i = 0; i < v1.getDegree(); i++) {
        if (v1.getAdjacentElementAt(i) == index2) {
          v1.setAdjacentElementAt(i, edgeVertexSplits.get(0).intValue());
          graph.get(edgeVertexSplits.get(0).intValue())
              .addElementAdjList(index1);
          updateV1 = true;
        }
      }

      for (int i = 0; i < v2.getDegree(); i++) {
        if (v2.getAdjacentElementAt(i) == index1) {
          v2.setAdjacentElementAt(i, edgeVertexSplits.get(splitSize - 1)
              .intValue());
          graph.get(edgeVertexSplits.get(splitSize - 1).intValue())
              .addElementAdjList(index2);
          updateV2 = true;
        }
      }

      for (int i = 0; i < splitSize - 1; i++) {
        int currentVertex = edgeVertexSplits.get(i).intValue();
        int nextVertex = edgeVertexSplits.get(i + 1).intValue();
        graph.get(currentVertex).addElementAdjList(nextVertex);
        graph.get(nextVertex).addElementAdjList(currentVertex);
      }
      if (!(updateV1 && updateV2)) {
        logger.log(Level.SEVERE, "inconsistent graph: (" + splitSize + ")"
            + index1 + " " + index2 + " "
            + v1.getAdjacencyList().toString() + " "
            + v2.getAdjacencyList().toString());
      }
    }
  }

  /**
   * Adds a portion of a pose as edges into constructedMap.
   */

  public void addToGraph(List<Vertex> constructedMap, List<Vertex> pose,
      Map<String, Integer> map, int startIndex, int endIndex) {
    for (int i = startIndex; i < endIndex; i++) {
      this.updateMap(constructedMap, map,
          new Edge(pose.get(i), pose.get(i + 1)));
    }

  }

  /**
   * Update the map for a pose/curve. Definition of black and white interval.
   */
  // @TODO(mahmuda): extract some shorter well-named methods.
  public void mapConstruction(List<Vertex> constructedMap, List<Edge> edges,
      Map<String, Integer> map, List<Vertex> pose, double eps,
      double altEps) {

    PriorityQueue<Edge> pq = new PriorityQueue<Edge>();

    for (int i = 0; i < edges.size(); i++) {
      this.computeNextInterval(edges.get(i), pose, 1, eps, altEps);
      if (!edges.get(i).getDone()) {
        pq.add(edges.get(i));
      }
    }
    try {

      // The whole curve will be added as an edge because no white
      // interval

      if (pq.isEmpty()) {

        logger.log(Level.FINER, MapMerging.curveName
            + " inserted as an edge");

        this.addToGraph(constructedMap, pose, map, 0, pose.size() - 1);

        logger.log(Level.FINER, MapMerging.curveName
            + " inserted as an edge");
        return;
      }

      Edge edge = pq.poll();

      double cend = edge.getCurveEnd();
      Edge cedge = edge;

      boolean first = true;

      int poseLimit = pose.size() - 1;

      // There is a black interval until edge.curveStart

      if (edge.getCurveStart() > 0) {

        logger.log(Level.FINER, MapMerging.curveName
            + " inserted as an edge until " + edge.getCurveStart());

        int index = (int) Math.floor(edge.getCurveStart());

        this.addToGraph(constructedMap, pose, map, 0, index);

        Line newLine = new Line(pose.get(index), pose.get(Math.min(poseLimit, index + 1)));
        double t = edge.getCurveStart()
            - Math.floor(edge.getCurveStart());
        this.updateMap(constructedMap, map, new Edge(pose.get(index),
            newLine.getVertex(t)));

        this.updateMap(constructedMap, map,
            new Edge(newLine.getVertex(t), edge.getLine()
                .getVertex(edge.getEdgeStart())));
        this.edgeSplit(constructedMap, map, edge, edge.getEdgeStart());
      }

      // the while loop will search through all the intervals until we
      // reach the end of the pose

      while (cend < pose.size()) {

        logger.log(Level.FINEST, MapMerging.curveName
            + " has white interval " + edge.getCurveStart() + " "
            + edge.getCurveEnd() + " " + cend);

        if (cend < edge.getCurveEnd()) {
          cend = edge.getCurveEnd();
          cedge = edge;
        }

        // Add entry from curve Start
        if (!first || (edge.getCurveStart() == 0)) {
          Vertex v = (edge.getEdgeStart() > 0.5) ? edge.getVertex2() : edge.getVertex1();

          int curveIndex = (int) Math.floor(edge.getCurveStart());

          v.addEntry(
            new Line(pose.get(curveIndex), pose.get(Math.min(poseLimit, curveIndex + 1)))
              .getVertex(edge.getCurveStart() - Math.floor(edge.getCurveStart()))
          );
        }

        // Add entry from curve End
        Vertex v = (edge.getEdgeEnd() > 0.5) ? edge.getVertex2() : edge.getVertex1();

        int curveIndex = (int) Math.ceil(edge.getCurveEnd());

        v.addEntry(
          new Line(pose.get(Math.max(0, curveIndex - 1)), pose.get(curveIndex))
            .getVertex(edge.getCurveEnd() - Math.floor(edge.getCurveEnd()))
        );

        if (edge.getCurveEnd() == pose.size() - 1) {
          logger.log(Level.FINER, MapMerging.curveName
              + " processing completed.");
          return;
        }

        first = false;

        this.computeNextInterval(edge, pose,
            edge.getCurveEndIndex() + 1, eps, altEps);

        if (!edge.getDone()) {
          pq.add(edge);
        }

        if (pq.isEmpty()) {
          logger.log(Level.FINER, MapMerging.curveName
              + " inserted as an edge from " + cend + " to end");

          int index = (int) Math.floor(cend);
          Line newLine = new Line(pose.get(index),
              pose.get(Math.min(poseLimit, index + 1)));
          double t = cend - Math.floor(cend);
          this.updateMap(
              constructedMap,
              map,
              new Edge(cedge.getLine().getVertex(
                  cedge.getEdgeEnd()), newLine.getVertex(t)));
          this.edgeSplit(constructedMap, map, cedge,
              cedge.getEdgeEnd());
          this.updateMap(constructedMap, map,
              new Edge(newLine.getVertex(t), pose.get(Math.min(poseLimit, index + 1))));
          this.addToGraph(constructedMap, pose, map, Math.min(poseLimit, index + 1),
              pose.size() - 1);

          return;
        }

        edge = pq.poll();

        if (edge.getCurveStart() > cend) {
          logger.log(Level.FINER, MapMerging.curveName
              + " inserted as an edge from " + cend + " to "
              + edge.getCurveStart());

          // need to add rest of the line segment

          int index = (int) Math.floor(cend);
          int indexStart = (int) Math.floor(edge.getCurveStart());
          Line newLine = new Line(pose.get(index),
              pose.get(Math.min(poseLimit, index + 1)));
          double t = cend - Math.floor(cend);

          this.updateMap(
              constructedMap,
              map,
              new Edge(cedge.getLine().getVertex(
                  cedge.getEdgeEnd()), newLine.getVertex(t)));
          this.edgeSplit(constructedMap, map, cedge,
              cedge.getEdgeEnd());

          if (index == indexStart) {
            this.updateMap(
                constructedMap,
                map,
                new Edge(newLine.getVertex(t),
                    newLine.getVertex(edge.getCurveStart()
                        - index)));
            index = (int) Math.floor(edge.getCurveStart());
            newLine = new Line(pose.get(index), pose.get(Math.min(poseLimit, index + 1)));
            t = edge.getCurveStart()
                - Math.floor(edge.getCurveStart());
          } else {
            this.updateMap(
                constructedMap,
                map,
                new Edge(newLine.getVertex(t), pose
                    .get(Math.min(poseLimit, index + 1))));

            this.addToGraph(constructedMap, pose, map, Math.min(poseLimit, index + 1),
                (int) Math.floor(edge.getCurveStart()));
            index = (int) Math.floor(edge.getCurveStart());
            newLine = new Line(pose.get(index), pose.get(Math.min(poseLimit, index + 1)));
            t = edge.getCurveStart()
                - Math.floor(edge.getCurveStart());
            this.updateMap(constructedMap, map,
                new Edge(pose.get(index), newLine.getVertex(t)));

          }
          this.updateMap(constructedMap, map,
              new Edge(newLine.getVertex(t), edge.getLine()
                  .getVertex(edge.getEdgeStart())));
          this.edgeSplit(constructedMap, map, edge,
              edge.getEdgeStart());
        }
      }
    } catch (Exception ex) {
      logger.log(Level.SEVERE, ex.toString());
      throw new RuntimeException(ex);
    }
    return;
  }

  public static void poseFromMap(List<CurveFile> poseFiles, List<Vertex> constructedMap, int startIndex, List<Integer> vertexHistory) {
    List<Vertex> curve = new ArrayList<Vertex>();

    Vertex vertex = constructedMap.get(startIndex);

    curve.add(new Vertex(vertex));

    vertexHistory.add(startIndex);

    double minX = vertex.getX(), maxX = vertex.getX(), minY = vertex.getY(), maxY = vertex.getY();

    int currIndex = startIndex;

    while (true) {
      vertex = constructedMap.get(currIndex);

      boolean next = false;
      int nextIndex = 0;

      for (int i = 0; i < vertex.getDegree(); i++) {
        int adjIndex = vertex.getAdjacentElementAt(i);

        if (vertexHistory.contains(adjIndex)) continue;

        Vertex nextVertex = new Vertex(constructedMap.get(adjIndex));

        boolean returned = (nextVertex.getX() >= minX) && (nextVertex.getX() <= maxX) && (nextVertex.getY() >= minY) && (nextVertex.getY() <= maxY);

        if (!next && !returned) {
          curve.add(nextVertex);

          vertexHistory.add(adjIndex);

          next = true;

          nextIndex = adjIndex;

          minX = Math.min(minX, nextVertex.getX());
          maxX = Math.max(maxX, nextVertex.getX());
          minY = Math.min(minY, nextVertex.getY());
          maxY = Math.max(maxY, nextVertex.getY());
        } else {
          MapMerging.poseFromMap(poseFiles, constructedMap, currIndex, vertexHistory);
        }
      }

      if (!next) break;

      currIndex = nextIndex;
    }

    poseFiles.add(CurveFile.readTrack(curve));
  }

  public static void readMapFiles(List<CurveFile> poseFiles, File folder) {
    System.out.print(LINE_FLUSH + "Reading partial map...");

    List<Vertex> constructedMap = new ArrayList<Vertex>();

    MapMerging.readFromFile(constructedMap, new HashMap<String, Integer>(), folder.getAbsolutePath());

    List<Integer> vertexHistory = new ArrayList<Integer>();

    int startIndex = 0;

    while (vertexHistory.size() < constructedMap.size()) {
      MapMerging.poseFromMap(poseFiles, constructedMap, startIndex, vertexHistory);

      while (vertexHistory.contains(startIndex)) {
        startIndex++;
      }
    }
  }

  /**
   * Constructs map from poses and returns string representation of the map.
   */

  public List<Vertex> constructMapMain(List<Vertex> constructedMap, Map<String, Integer> map, List<CurveFile> poseFiles, double eps, double altEps) {
    try {
      double length = 0;

      // generate list of files in the folder to process
      for (int k = 0; k < poseFiles.size(); k++) {
        CurveFile poseFile = poseFiles.get(k);
        Long startTime = System.currentTimeMillis();
        MapMerging.curveid = k;
        MapMerging.curveName = poseFile.getFileName();

        length += poseFile.getLength();

        if (poseFile.getPose().size() < 2) {
          continue;
        }

        List<Edge> edges = new ArrayList<Edge>();

        List<Integer> vertexHistory = new ArrayList<Integer>();

        for (int i = 0; i < constructedMap.size(); i++) {
          Vertex v = constructedMap.get(i);

          for (int j = 0; j < v.getDegree(); j++) {
            int index = v.getAdjacentElementAt(j);

            if ((index == i) || vertexHistory.contains(index)) continue;

            Vertex w = constructedMap.get(index);

            if (!poseFile.enclosesVertex(v) && !poseFile.enclosesVertex(w)) continue;

            edges.add(new Edge(v, w));
          }

          vertexHistory.add(i);
        }

        this.mapConstruction(constructedMap, edges, map, poseFile.getPose(), eps, altEps);
        this.commitEdgeSplits(edges, map, constructedMap);

        logger.info("k :" + k + " " + MapMerging.curveName + " "
            + length + " :"
            + (System.currentTimeMillis() - startTime) / 60000.00);

        System.out.print(LINE_FLUSH + "Rebuilding map: " + k + "/" + poseFiles.size() + " tracks");
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.toString());
      throw new RuntimeException(e);
    }
    return constructedMap;
  }

  public static void main(String args[]) {
    MapMerging mapConstruction = new MapMerging();

    // path to the folder that contains input tracks.
    String inputPath = args[0];

    // path to the folder where the output will be written.
    String outputpath = args[1];

    // epsilon; see the paper for detail
    double eps = Double.parseDouble(args[2]);

    // if the input files contains altitude information
    boolean hasAltitude = Boolean.parseBoolean(args[3]);

    // minimum altitude difference between two streets.
    double altEps;
    if (args.length > 4) {
      altEps = Double.parseDouble(args[4]);
    } else {
      altEps = 4.0;
    }

    List<CurveFile> poseFiles = new ArrayList<CurveFile>();

    List<Vertex> constructedMap = new ArrayList<Vertex>();
    // map contains mapping between vertex keys and their indices in
    // constructedMap
    Map<String, Integer> map = new HashMap<String, Integer>();

    boolean first = false;

    for (File folder : new File(inputPath).listFiles()) {
      if (!folder.isDirectory()) continue;

      if (!first) {
        MapMerging.readFromFile(constructedMap, map, folder.getAbsolutePath());

        first = true;
      } else {
        MapMerging.readMapFiles(poseFiles, folder);
      }
    }

    mapConstruction.constructMapMain(constructedMap, map, poseFiles, eps, altEps);

    System.out.print(LINE_FLUSH + "Exporting merged map...");

    MapMerging.writeToFile(constructedMap, outputpath);

    System.out.print(LINE_FLUSH + "Merged maps!\n");
  }
}
