/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc;

import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.util.*;
import java.util.*;

/**
 * Superclass for our implementations of geoloc.Projection.
 * <p/>
 * <p>
 * All subclasses must:
 * <ul>
 * <li>override equals() and return true when all parameters are equal
 * <li>create "atts" list of parameters as string-valued Attribute pairs
 * <li>implement abstract methods
 * </ul>
 * <p/>
 * If possible, set defaultmapArea to some reasonable world coord bounding box
 * otherwise, provide a way for the user to specify it when a specific projection
 * is created.
 * <p/>
 * <p>
 * Note on "false_easting" and "fale_northing" projection parameters:
 * <ul>
 * <li>false_easting(northing) = The value added to all x (y) values in the rectangular coordinates for a map
 * projection.
 * This value frequently is assigned to eliminate negative numbers.
 * Expressed in the unit of measure identified in Planar Coordinate Units.
 * <li>We dont currently use, assuming that the x and y are just fine as negetive numbers.
 * </ul>
 *
 * @author John Caron
 * @see Projection
 * @deprecated only use Projection interface in 6; will not implement Serializable in ver6
 */
@Deprecated
public abstract class ProjectionImpl implements Projection, java.io.Serializable {
  /** @deprecated not public. */
  public static final double EARTH_RADIUS = Earth.WGS84_EARTH_RADIUS_KM;

  /** @deprecated not public. */
  public static final int INDEX_LAT = 0;

  /** @deprecated not public. */
  public static final int INDEX_LON = 1;

  /** @deprecated not public. */
  public static final int INDEX_X = 0;

  /** @deprecated not public. */
  public static final int INDEX_Y = 1;

  /** @deprecated not public. */
  protected static final double TOLERANCE = 1.0e-6;

  /** @deprecated use Math.PI */
  public static final double PI = Math.PI;

  /** @deprecated use Math.PI/2 */
  public static final double PI_OVER_2 = Math.PI / 2.0;

  /** @deprecated use Math.PI/4 */
  public static final double PI_OVER_4 = Math.PI / 4.0;

  ///////////////////////////////////////////////////////////////////////

  /**
   * name of this projection.
   */
  protected String name; // LOOK should be final, IDV needs setName()

  /**
   * flag for latlon
   */
  protected final boolean isLatLon;

  /**
   * list of attributes
   */
  protected final List<Parameter> atts = new ArrayList<>();

  /**
   * default map area
   */
  protected ProjectionRect defaultMapArea = new ProjectionRect();

  /**
   * copy constructor - avoid clone !!
   *
   * @return a copy of this Projection.
   *         TODO return Projection in ver6
   */
  public abstract ProjectionImpl constructCopy();

  protected ProjectionImpl(String name, boolean isLatLon) {
    this.name = name;
    this.isLatLon = isLatLon;
  }

  /**
   * Get the name of the type of the projection.
   *
   * @return the class name
   */
  public String getClassName() {
    String className = getClass().getName();
    int index = className.lastIndexOf(".");
    if (index >= 0) {
      className = className.substring(index + 1);
    }
    return className;
  }

  /**
   * Get a string representation of the projection parameters
   *
   * @return string representation of the projection parameters
   */
  public abstract String paramsToString();

  /**
   * Get the label to be used in the gui for this type of projection.
   * This defaults to call getClassName
   *
   * @return Type label
   */
  public String getProjectionTypeLabel() {
    return getClassName();
  }

  /**
   * Convert a LatLonPoint to projection coordinates
   *
   * @param latlon convert from these lat, lon coordinates
   * @param destPoint the object to write to
   * @return the given destPoint
   * @deprecated use latLonToProj(LatLonPoint latLon)
   */
  @Deprecated
  public abstract ProjectionPoint latLonToProj(LatLonPoint latlon, ProjectionPointImpl destPoint);

  /**
   * Convert a LatLonPoint to projection coordinates
   * Note: a new object is now created on each call for the return value, as of 4.0.46
   *
   * @param latLon convert from these lat, lon coordinates
   * @return ProjectionPoint convert to these projection coordinates
   */
  public ProjectionPoint latLonToProj(LatLonPoint latLon) {
    return latLonToProj(latLon, new ProjectionPointImpl());
  }

  /**
   * Convert projection coordinates to a LatLonPoint
   * Note: a new object is not created on each call for the return value.
   *
   * @param ppt convert from these projection coordinates
   * @param destPoint the object to write to
   * @return LatLonPoint convert to these lat/lon coordinates
   * @deprecated use projToLatLon(ProjectionPoint ppt)
   */
  @Deprecated
  public abstract LatLonPoint projToLatLon(ProjectionPoint ppt, LatLonPointImpl destPoint);

  /**
   * Convert projection coordinates to a LatLonPoint
   * Note: a new object is now created on each call for the return value, as of 4.0.46
   *
   * @param ppt convert from these projection coordinates
   * @return LatLonPoint convert to these lat/lon coordinates
   */
  public LatLonPoint projToLatLon(ProjectionPoint ppt) {
    return projToLatLon(ppt, new LatLonPointImpl());
  }

  /**
   * Does the line between these two points cross the projection "seam".
   *
   * @param pt1 the line goes between these two points
   * @param pt2 the line goes between these two points
   * @return false if there is no seam
   */
  public abstract boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2);

  /**
   * Returns true if this represents the same Projection as proj.
   *
   * @param proj projection in question
   * @return true if this represents the same Projection as proj.
   */
  public abstract boolean equals(Object proj);

  /**
   * Get the name of this specific projection (also see getClassName)
   *
   * @return name of the projection
   */
  public String getName() {
    return name;
  }

  /** @deprecated use builder */
  @Deprecated
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Get parameters as list of ucar.unidata.util.Parameter
   *
   * @return List of parameters
   */
  public List<Parameter> getProjectionParameters() {
    return atts;
  }

  /** @deprecated do not use */
  @Deprecated
  public Parameter findProjectionParameter(String want) {
    for (Parameter p : atts) {
      if (p.getName().equals(want))
        return p;
    }
    return null;
  }

  /**
   * Add an attribute to this projection
   *
   * @param name name of the attribute
   * @param value attribute value as a string
   */
  protected void addParameter(String name, String value) {
    atts.add(new Parameter(name, value));
  }

  /**
   * Add an attribute to this projection
   *
   * @param name name of the attribute
   * @param value attribute value as a double
   */
  protected void addParameter(String name, double value) {
    atts.add(new Parameter(name, value));
  }

  /**
   * Add an attribute to this projection
   *
   * @param p specify as a Parameter
   */
  protected void addParameter(Parameter p) {
    atts.add(p);
  }

  /**
   * Is this the lat/lon Projection ?
   *
   * @return true if it is the lat/lon Projection
   */
  public boolean isLatLon() {
    return isLatLon;
  }

  /**
   * Get a header for display.
   *
   * @return human readable header for display
   */
  public static String getHeader() {
    StringBuilder headerB = new StringBuilder(60);
    headerB.append("Name");
    Format.tab(headerB, 20, true);
    headerB.append("Class");
    Format.tab(headerB, 40, true);
    headerB.append("Parameters");
    return headerB.toString();
  }

  /**
   * Get a String representation of this projection.
   *
   * @return the name of the projection. This is what gets
   *         displayed when you add the projection object to
   *         a UI widget (e.g. label, combobox)
   */
  public String toString() {
    return getName();
  }

  /**
   * Get a reasonable bounding box for this projection.
   *
   * @return reasonable bounding box
   */
  public ProjectionRect getDefaultMapArea() {
    return defaultMapArea;
  }

  /**
   * Get the bounding box in lat/lon.
   *
   * @return the LatLonRectangle for the bounding box
   */
  public LatLonRect getDefaultMapAreaLL() {
    return projToLatLonBB(defaultMapArea);
  }

  /**
   * Set a reasonable bounding box for this specific projection.
   * Projections are typically specific to an area of the world;
   * theres no bounding box that works for all projections.
   *
   * @param bb bounding box
   * @deprecated use builder
   */
  @Deprecated
  public void setDefaultMapArea(ProjectionRect bb) {
    if (bb == null)
      return;
    defaultMapArea = new ProjectionRect(bb);
  }

  //////// convenience routines

  ///////////////////////////////////////////////////////////////////////////////////
  // optimizations for doing double and float arrays

  /**
   * Convert projection coordinates to lat/lon coordinates.
   *
   * @param from array of projection coordinates: from[2][n],
   *        where from[0][i], from[1][i] is the x, y coordinate
   *        of the ith point
   * @return resulting array of lat/lon coordinates, where to[0][i], to[1][i]
   *         is the lat,lon coordinate of the ith point
   * @deprecated use Projections.latLonToProj(Projection proj, ...)
   */
  @Deprecated
  public double[][] projToLatLon(double[][] from) {
    return projToLatLon(from, new double[2][from[0].length]);
  }

  /**
   * Convert projection coordinates to lat/lon coordinate.
   *
   * @param from array of projection coordinates: from[2][n], where
   *        (from[0][i], from[1][i]) is the (x, y) coordinate
   *        of the ith point
   * @param to resulting array of lat/lon coordinates: to[2][n] where
   *        (to[0][i], to[1][i]) is the (lat, lon) coordinate of
   *        the ith point
   * @return the "to" array
   * @deprecated use Projections.latLonToProj(Projection proj, ...)
   */
  @Deprecated
  public double[][] projToLatLon(double[][] from, double[][] to) {
    if ((from == null) || (from.length != 2)) {
      throw new IllegalArgumentException(
          "ProjectionImpl.projToLatLon:" + "null array argument or wrong dimension (from)");
    }
    if ((to == null) || (to.length != 2)) {
      throw new IllegalArgumentException(
          "ProjectionImpl.projToLatLon:" + "null array argument or wrong dimension (to)");
    }

    if (from[0].length != to[0].length) {
      throw new IllegalArgumentException("ProjectionImpl.projToLatLon:" + "from array not same length as to array");
    }

    for (int i = 0; i < from[0].length; i++) {
      LatLonPoint endL = projToLatLon(from[0][i], from[1][i]);
      to[0][i] = endL.getLatitude();
      to[1][i] = endL.getLongitude();
    }

    return to;
  }

  /**
   * Convert projection coordinates to lat/lon coordinates.
   *
   * @param from array of projection coordinates: from[2][n],
   *        where from[0][i], from[1][i] is the x, y coordinate
   *        of the ith point
   * @return resulting array of lat/lon coordinates, where to[0][i], to[1][i]
   *         is the lat,lon coordinate of the ith point
   * @deprecated use Projections.latLonToProj(Projection proj, ...)
   */
  @Deprecated
  public float[][] projToLatLon(float[][] from) {
    return projToLatLon(from, new float[2][from[0].length]);
  }

  /**
   * Convert projection coordinates to lat/lon coordinate.
   *
   * @param from array of projection coordinates: from[2][n], where
   *        (from[0][i], from[1][i]) is the (x, y) coordinate
   *        of the ith point
   * @param to resulting array of lat/lon coordinates: to[2][n] where
   *        (to[0][i], to[1][i]) is the (lat, lon) coordinate of
   *        the ith point
   * @return the "to" array
   * @deprecated use Projections.latLonToProj(Projection proj, ...)
   */
  @Deprecated
  public float[][] projToLatLon(float[][] from, float[][] to) {
    if ((from == null) || (from.length != 2)) {
      throw new IllegalArgumentException(
          "ProjectionImpl.projToLatLon:" + "null array argument or wrong dimension (from)");
    }
    if ((to == null) || (to.length != 2)) {
      throw new IllegalArgumentException(
          "ProjectionImpl.projToLatLon:" + "null array argument or wrong dimension (to)");
    }

    if (from[0].length != to[0].length) {
      throw new IllegalArgumentException("ProjectionImpl.projToLatLon:" + "from array not same length as to array");
    }

    for (int i = 0; i < from[0].length; i++) {
      ProjectionPoint ppi = ProjectionPoint.create(from[0][i], from[1][i]);
      LatLonPoint llpi = projToLatLon(ppi);
      to[0][i] = (float) llpi.getLatitude();
      to[1][i] = (float) llpi.getLongitude();
    }

    return to;
  }

  /**
   * Convert lat/lon coordinates to projection coordinates.
   *
   * @param from array of lat/lon coordinates: from[2][n],
   *        where from[0][i], from[1][i] is the (lat,lon)
   *        coordinate of the ith point
   * @return resulting array of projection coordinates, where to[0][i],
   *         to[1][i] is the (x,y) coordinate of the ith point
   * @deprecated use Projections.latLonToProj(Projection proj, ...)
   */
  @Deprecated
  public double[][] latLonToProj(double[][] from) {
    return latLonToProj(from, new double[2][from[0].length]);
  }

  /**
   * Convert lat/lon coordinates to projection coordinates.
   *
   * @param from array of lat/lon coordinates: from[2][n], where
   *        (from[0][i], from[1][i]) is the (lat,lon) coordinate
   *        of the ith point
   * @param to resulting array of projection coordinates: to[2][n]
   *        where (to[0][i], to[1][i]) is the (x,y) coordinate
   *        of the ith point
   * @return the "to" array
   * @deprecated use Projections.latLonToProj(Projection proj, ...)
   */
  @Deprecated
  public double[][] latLonToProj(double[][] from, double[][] to) {
    return latLonToProj(from, to, INDEX_LAT, INDEX_LON);
  }

  /**
   * Convert lat/lon coordinates to projection coordinates.
   *
   * @param from array of lat/lon coordinates: from[2][n], where
   *        (from[latIndex][i], from[lonIndex][i]) is the (lat,lon)
   *        coordinate of the ith point
   * @param latIndex index of lat coordinate; must be 0 or 1
   * @param lonIndex index of lon coordinate; must be 0 or 1
   * @return resulting array of projection coordinates: to[2][n] where
   *         (to[0][i], to[1][i]) is the (x,y) coordinate of the ith point
   * @deprecated use Projections.latLonToProj(Projection proj, ...)
   */
  @Deprecated
  public double[][] latLonToProj(double[][] from, int latIndex, int lonIndex) {
    return latLonToProj(from, new double[2][from[0].length], latIndex, lonIndex);
  }

  /**
   * Convert lat/lon coordinates to projection coordinates.
   *
   * @param from array of lat/lon coordinates: from[2][n], where
   *        (from[latIndex][i], from[lonIndex][i]) is the (lat,lon)
   *        coordinate of the ith point
   * @param to resulting array of projection coordinates: to[2][n]
   *        where (to[0][i], to[1][i]) is the (x,y) coordinate of
   *        the ith point
   * @param latIndex index of lat coordinate; must be 0 or 1
   * @param lonIndex index of lon coordinate; must be 0 or 1
   * @return the "to" array
   * @deprecated use Projections.latLonToProj(Projection proj, ...)
   */
  @Deprecated
  public double[][] latLonToProj(double[][] from, double[][] to, int latIndex, int lonIndex) {
    if ((from == null) || (from.length != 2)) {
      throw new IllegalArgumentException(
          "ProjectionImpl.latLonToProj:" + "null array argument or wrong dimension (from)");
    }
    if ((to == null) || (to.length != 2)) {
      throw new IllegalArgumentException(
          "ProjectionImpl.latLonToProj:" + "null array argument or wrong dimension (to)");
    }

    if (from[0].length != to[0].length) {
      throw new IllegalArgumentException("ProjectionImpl.latLonToProj:" + "from array not same length as to array");
    }

    for (int i = 0; i < from[0].length; i++) {
      LatLonPoint llpi = LatLonPoint.create(from[latIndex][i], from[lonIndex][i]);
      ProjectionPoint ppi = latLonToProj(llpi);
      to[0][i] = ppi.getX();
      to[1][i] = ppi.getY();
    }
    return to;
  }

  /**
   * Convert lat/lon coordinates to projection coordinates.
   *
   * @param from array of lat/lon coordinates: from[2][n],
   *        where from[0][i], from[1][i] is the (lat,lon)
   *        coordinate of the ith point
   * @return resulting array of projection coordinates, where to[0][i],
   *         to[1][i] is the (x,y) coordinate of the ith point
   * @deprecated use Projections.latLonToProj(Projection proj, ...)
   */
  @Deprecated
  public float[][] latLonToProj(float[][] from) {
    return latLonToProj(from, new float[2][from[0].length]);
  }

  /**
   * Convert lat/lon coordinates to projection coordinates.
   *
   * @param from array of lat/lon coordinates: from[2][n], where
   *        (from[0][i], from[1][i]) is the (lat,lon) coordinate
   *        of the ith point
   * @param to resulting array of projection coordinates: to[2][n]
   *        where (to[0][i], to[1][i]) is the (x,y) coordinate
   *        of the ith point
   * @return the "to" array
   * @deprecated use Projections.latLonToProj(Projection proj, ...)
   */
  @Deprecated
  public float[][] latLonToProj(float[][] from, float[][] to) {
    return latLonToProj(from, to, INDEX_LAT, INDEX_LON);
  }

  /**
   * Convert lat/lon coordinates to projection coordinates.
   *
   * @param from array of lat/lon coordinates: from[2][n], where
   *        (from[latIndex][i], from[lonIndex][i]) is the (lat,lon)
   *        coordinate of the ith point
   * @param latIndex index of lat coordinate; must be 0 or 1
   * @param lonIndex index of lon coordinate; must be 0 or 1
   * @return resulting array of projection coordinates: to[2][n] where
   *         (to[0][i], to[1][i]) is the (x,y) coordinate of the ith point
   * @deprecated use Projections.latLonToProj(Projection proj, ...)
   */
  @Deprecated
  public float[][] latLonToProj(float[][] from, int latIndex, int lonIndex) {
    return latLonToProj(from, new float[2][from[0].length], latIndex, lonIndex);
  }


  /**
   * Convert lat/lon coordinates to projection coordinates.
   *
   * @param from array of lat/lon coordinates: from[2][n], where
   *        (from[latIndex][i], from[lonIndex][i]) is the (lat,lon)
   *        coordinate of the ith point
   * @param to resulting array of projection coordinates: to[2][n]
   *        where (to[0][i], to[1][i]) is the (x,y) coordinate of
   *        the ith point
   * @param latIndex index of lat coordinate; must be 0 or 1
   * @param lonIndex index of lon coordinate; must be 0 or 1
   * @return the "to" array
   * @deprecated use Projections.latLonToProj(Projection proj, ...)
   */
  @Deprecated
  public float[][] latLonToProj(float[][] from, float[][] to, int latIndex, int lonIndex) {
    // ucar.unidata.util.Misc.printStack ("latLonToProj-" + this + " size=" + from[0].length, 4, null);

    if ((from == null) || (from.length != 2)) {
      throw new IllegalArgumentException(
          "ProjectionImpl.latLonToProj:" + "null array argument or wrong dimension (from)");
    }
    if ((to == null) || (to.length != 2)) {
      throw new IllegalArgumentException(
          "ProjectionImpl.latLonToProj:" + "null array argument or wrong dimension (to)");
    }

    if (from[0].length != to[0].length) {
      throw new IllegalArgumentException("ProjectionImpl.latLonToProj:" + "from array not same length as to array");
    }

    for (int i = 0; i < from[0].length; i++) {
      LatLonPoint llpi = LatLonPoint.create(from[latIndex][i], from[lonIndex][i]);
      ProjectionPoint ppi = latLonToProj(llpi);
      to[0][i] = (float) ppi.getX();
      to[1][i] = (float) ppi.getY();
    }

    return to;
  }

  //////////////////////////////////////////////////////////////////////

  // Allow subclasses to override.
  @Override
  public ProjectionRect latLonToProjBB(LatLonRect latlonRect) {
    if (isLatLon) {
      LatLonProjection llp = (LatLonProjection) this;
      llp.setCenterLon(latlonRect.getCenterLon()); // LOOK side effect BAD !!
    }

    LatLonPoint ll = latlonRect.getLowerLeftPoint();
    LatLonPoint ur = latlonRect.getUpperRightPoint();
    ProjectionPoint w1 = latLonToProj(ll);
    ProjectionPoint w2 = latLonToProj(ur);

    // make bounding box out of those two corners
    ProjectionRect world = new ProjectionRect(w1.getX(), w1.getY(), w2.getX(), w2.getY());

    LatLonPoint la = LatLonPoint.create(ur.getLatitude(), ll.getLongitude());
    LatLonPoint lb = LatLonPoint.create(ll.getLatitude(), ur.getLongitude());

    // now extend if needed to the other two corners
    world.add(latLonToProj(la));
    world.add(latLonToProj(lb));

    return world;
  }

  // Allow subclasses to override.
  @Override
  public LatLonRect projToLatLonBB(ProjectionRect bb) {
    // look at all 4 corners of the bounding box
    LatLonPoint llpt = projToLatLon(bb.getLowerLeftPoint());
    LatLonPoint lrpt = projToLatLon(bb.getLowerRightPoint());
    LatLonPoint urpt = projToLatLon(bb.getUpperRightPoint());
    LatLonPoint ulpt = projToLatLon(bb.getUpperLeftPoint());

    // Check if grid contains poles.
    boolean includesNorthPole = false;
    /*
     * int[] resultNP;
     * findXYindexFromLatLon(90.0, 0, resultNP);
     * if (resultNP[0] != -1 && resultNP[1] != -1)
     * includesNorthPole = true;
     */
    boolean includesSouthPole = false;
    /*
     * int[] resultSP = new int[2];
     * findXYindexFromLatLon(-90.0, 0, resultSP);
     * if (resultSP[0] != -1 && resultSP[1] != -1)
     * includesSouthPole = true;
     */

    LatLonRect llbb;

    if (includesNorthPole && !includesSouthPole) {
      llbb = new LatLonRect(llpt, LatLonPoint.create(90.0, 0.0)); // ??? lon=???
      llbb.extend(lrpt);
      llbb.extend(urpt);
      llbb.extend(ulpt);
      // OR
      // llbb.extend( new LatLonRect( llpt, lrpt ));
      // llbb.extend( new LatLonRect( lrpt, urpt ) );
      // llbb.extend( new LatLonRect( urpt, ulpt ) );
      // llbb.extend( new LatLonRect( ulpt, llpt ) );
    } else if (includesSouthPole && !includesNorthPole) {
      llbb = new LatLonRect(llpt, LatLonPoint.create(-90.0, -180.0)); // ??? lon=???
      llbb.extend(lrpt);
      llbb.extend(urpt);
      llbb.extend(ulpt);

    } else {
      double latMin = Math.min(llpt.getLatitude(), lrpt.getLatitude());
      double latMax = Math.max(ulpt.getLatitude(), urpt.getLatitude());

      // longitude is a bit tricky as usual
      double lonMin = getMinOrMaxLon(llpt.getLongitude(), ulpt.getLongitude(), true);
      double lonMax = getMinOrMaxLon(lrpt.getLongitude(), urpt.getLongitude(), false);

      LatLonPoint min = LatLonPoint.create(latMin, lonMin);
      LatLonPoint max = LatLonPoint.create(latMax, lonMax);
      llbb = new LatLonRect(min, max);
    }

    return llbb;
  }

  /**
   * Alternate way to calculate latLonToProjBB, originally in GridCoordSys.
   * Difficult to do this in a general way.
   * TODO evaluate if this is better than latLonToProjBB
   *
   * @param latlonRect desired lat/lon rectangle
   * @return a ProjectionRect
   */
  ProjectionRect latLonToProjBB2(LatLonRect latlonRect) {
    double minx, maxx, miny, maxy;

    LatLonPoint llpt = latlonRect.getLowerLeftPoint();
    LatLonPoint urpt = latlonRect.getUpperRightPoint();
    LatLonPoint lrpt = latlonRect.getLowerRightPoint();
    LatLonPoint ulpt = latlonRect.getUpperLeftPoint();

    if (isLatLon()) {
      minx = getMinOrMaxLon(llpt.getLongitude(), ulpt.getLongitude(), true);
      miny = Math.min(llpt.getLatitude(), lrpt.getLatitude());
      maxx = getMinOrMaxLon(urpt.getLongitude(), lrpt.getLongitude(), false);
      maxy = Math.min(ulpt.getLatitude(), urpt.getLatitude());

    } else {
      ProjectionPoint ll = latLonToProj(llpt);
      ProjectionPoint ur = latLonToProj(urpt);
      ProjectionPoint lr = latLonToProj(lrpt);
      ProjectionPoint ul = latLonToProj(ulpt);

      minx = Math.min(ll.getX(), ul.getX());
      miny = Math.min(ll.getY(), lr.getY());
      maxx = Math.max(ur.getX(), lr.getX());
      maxy = Math.max(ul.getY(), ur.getY());
    }

    return new ProjectionRect(minx, miny, maxx, maxy);
  }

  private double getMinOrMaxLon(double lon1, double lon2, boolean wantMin) {
    double midpoint = (lon1 + lon2) / 2;
    lon1 = LatLonPoints.lonNormal(lon1, midpoint);
    lon2 = LatLonPoints.lonNormal(lon2, midpoint);

    return wantMin ? Math.min(lon1, lon2) : Math.max(lon1, lon2);
  }

  /**
   * Convert a world coordinate bounding box to a lat/lon bounding box,
   * by finding the minimum enclosing box.
   *
   * @param world input world coordinate bounding box
   * @return minimum enclosing box in lat,lon coordinates.
   * @deprecated do not use
   */
  @Deprecated
  public LatLonRect projToLatLonBBold(ProjectionRect world) {
    ProjectionPoint min = world.getMinPoint();
    ProjectionPoint max = world.getMaxPoint();
    LatLonRect llbb;

    // make bounding box out of the min, max corners
    LatLonPoint llmin = projToLatLon(min);
    LatLonPoint llmax = projToLatLon(max);
    llbb = new LatLonRect(llmin, llmax);

    /*
     * double lona = la.getLongitude();
     * double lonb = lb.getLongitude();
     *
     * if (((lona < lonb) && (lonb - lona <= 180.0))
     * || ((lona > lonb) && (lona - lonb >= 180.0))) {
     * llbb = new LatLonRect(la, lb);
     * } else {
     * llbb = new LatLonRect(lb, la);
     * }
     */

    ProjectionPoint w1 = ProjectionPoint.create(min.getX(), max.getY());
    ProjectionPoint w2 = ProjectionPoint.create(max.getX(), min.getY());

    // now extend if needed using the other two corners
    llmin = projToLatLon(w1);
    llbb.extend(llmin);

    llmax = projToLatLon(w2);
    llbb.extend(llmax);

    return llbb;
  }

}

