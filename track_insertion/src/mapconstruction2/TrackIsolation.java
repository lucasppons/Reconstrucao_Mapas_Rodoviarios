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
 * Author: Mahmuda Ahmed Filename: MapConstruction.java
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
class TrackFile {
  String fileName;
  ArrayList<Vertex> curve;
  double minX;
  double minY;
  double maxX;
  double maxY;

  TrackFile() {
    this.fileName = "";
    this.curve = new ArrayList<Vertex>();

    minX = minY = Double.POSITIVE_INFINITY;
    maxX = maxY = Double.NEGATIVE_INFINITY;
  }

  TrackFile(String curveName, ArrayList<Vertex> curve) {
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

  public static TrackFile readFile(File inputFile, boolean hasAltitude) {
    TrackFile poseFile = new TrackFile();
    poseFile.fileName = inputFile.getName();
    String str = "";

    try {
      BufferedReader in = new BufferedReader(new FileReader(
          inputFile.getAbsolutePath()));
      double x, y, z;

      while ((str = in.readLine()) != null) {
        StringTokenizer strToken = new StringTokenizer(str);
        // strToken.nextToken();
        // track file in "x y timestamp" or "x y z timestamp" format

        x = Double.parseDouble(strToken.nextToken());
        y = Double.parseDouble(strToken.nextToken());

        if (hasAltitude) {
          z = Double.parseDouble(strToken.nextToken());
        } else {
          z = 0.0;
        }

        Vertex newPoint = new Vertex(x, y, z);

        if (poseFile.curve.size() > 0) {
          Vertex lastPoint = poseFile.curve.get(poseFile.curve.size() - 1);

          if (newPoint.dist(lastPoint) < 0.001) {
            poseFile.curve.remove(poseFile.curve.size() - 1);
            newPoint = (new Line(lastPoint, newPoint)).getVertex(0.5);
          }
        }

        poseFile.curve.add(newPoint);

        poseFile.minX = Math.min(poseFile.minX, newPoint.getX());
        poseFile.minY = Math.min(poseFile.minY, newPoint.getY());
        poseFile.maxX = Math.max(poseFile.maxX, newPoint.getX());
        poseFile.maxY = Math.max(poseFile.maxY, newPoint.getY());
      }

      poseFile.minX -= 0.1;
      poseFile.minY -= 0.1;
      poseFile.maxX += 0.1;
      poseFile.maxY += 0.1;

      in.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return poseFile;
  }
}

/**
 * An object that takes a set of poses as input, construct graph and write two
 * files one for vertices and one for edges.
 */
public class TrackIsolation {

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
      BufferedWriter bvertex = new BufferedWriter(new FileWriter(fileName
          + "vertices.txt"));


      for (int i = 0; i < vList.size(); i++) {
        Vertex v = vList.get(i);
        bvertex.write(i + "," + v.getX() + "," + v.getY() +","+ v.getZ() + "," + v.getOnlineAvg() + "\n");
      }

      bvertex.close();
    } catch (Exception ex) {
      System.out.println(ex.toString());
    }
  }

  public static void readFromFile(List<Vertex> constructedMap, String fileName) {
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

        this.setEndPointsOnEdge(edge, startIndex, i, cstart, vstart);
        return;
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
   * Isolate track
   */
  // @TODO(mahmuda): extract some shorter well-named methods.
  public void isolateTrack(List<Vertex> constructedMap, TrackFile track, double eps,
      double altEps) {
    List<Edge> edges = new ArrayList<Edge>();

    List<Integer> vertexHistory = new ArrayList<Integer>();

    for (int i = 0; i < constructedMap.size(); i++) {
      Vertex v = constructedMap.get(i);

      for (int j = 0; j < v.getDegree(); j++) {
        int index = v.getAdjacentElementAt(j);

        if ((index == i) || vertexHistory.contains(index)) continue;

        Vertex w = constructedMap.get(index);

        if (!track.enclosesVertex(v) && !track.enclosesVertex(w)) continue;

        edges.add(new Edge(v, w));
      }

      vertexHistory.add(i);
    }

    List<Vertex> pose = track.getPose();

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
        return;
      }

      Edge edge = pq.poll();

      double cend = edge.getCurveEnd();
      Edge cedge = edge;

      boolean first = true;

      // the while loop will search through all the intervals until we
      // reach the end of the pose

      while (cend < pose.size()) {

        logger.log(Level.FINEST, MapConstruction.curveName
            + " has white interval " + edge.getCurveStart() + " "
            + edge.getCurveEnd() + " " + cend);

        if (cend < edge.getCurveEnd()) {
          cend = edge.getCurveEnd();
          cedge = edge;
        }

        // Add entry from curve Start
        if (!first || (edge.getCurveStart() == 0)) {
          Vertex v = pose.get((int) Math.floor(edge.getCurveStart()));

          // if (v.getEntryCount() == 0) {
            v.addEntry(
              edge.getLine().getVertex(edge.getEdgeStart() - Math.floor(edge.getEdgeStart()))
            );
          // }
        }

        // Add entry from curve End
        Vertex v = pose.get((int) Math.ceil(edge.getCurveEnd()));

        // if (v.getEntryCount() == 0) {
          v.addEntry(
            edge.getLine().getVertex(edge.getEdgeEnd() - Math.floor(edge.getEdgeEnd()))
          );
        // }

        if (edge.getCurveEnd() == pose.size() - 1) {
          logger.log(Level.FINER, MapConstruction.curveName
              + " processing completed.");
          return;
        }

        first = false;

        this.computeNextInterval(edge, pose,
            edge.getCurveEndIndex() + 1, eps, altEps);

        if (!edge.getDone()) {
          pq.add(edge);
        }

        edge = pq.poll();
      }
    } catch (Exception ex) {
      logger.log(Level.SEVERE, ex.toString());
      throw new RuntimeException(ex);
    }
    return;
  }

  public static void main(String args[]) {
    TrackIsolation trackIsolation = new TrackIsolation();

    // path to the folder that contains input tracks.
    String inputPath = args[0];

    String trackPath = args[1];

    // path to the folder where the output will be written.
    String outputpath = args[2];

    // epsilon; see the paper for detail
    double eps = Double.parseDouble(args[3]);

    // if the input files contains altitude information
    boolean hasAltitude = Boolean.parseBoolean(args[4]);

    // minimum altitude difference between two streets.
    double altEps;
    if (args.length > 4) {
      altEps = Double.parseDouble(args[5]);
    } else {
      altEps = 4.0;
    }

    List<Vertex> constructedMap = new ArrayList<Vertex>();

    TrackFile track = TrackFile.readFile(new File(trackPath), hasAltitude);

    System.out.print(LINE_FLUSH + "Reading rebuilt map...");

    TrackIsolation.readFromFile(constructedMap, inputPath);

    System.out.print(LINE_FLUSH + "Isolating track...");

    trackIsolation.isolateTrack(constructedMap, track, eps, altEps);

    System.out.print(LINE_FLUSH + "Exporting isolated track map...");

    TrackIsolation.writeToFile(track.getPose(), outputpath);

    System.out.print(LINE_FLUSH + "Isolated track!\n");
  }
}
