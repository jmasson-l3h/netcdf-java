/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point.writer;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.ParameterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.*;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.ft.*;
import ucar.nc2.ft.point.StationPointFeature;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.util.CancelTask;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.Nc4ChunkingStrategy;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Write Point Feature Collections into netcdf3/4 files in CF 1.6 point obs conventions.
 * <ul>
 * <li>netcdf3: use indexed ragged array representation</li>
 * </ul>
 * 
 * @deprecated use writer2
 */
@Deprecated
public abstract class CFPointWriter implements Closeable {
  private static final Logger logger = LoggerFactory.getLogger(CFPointWriter.class);

  public static final String recordName = "obs";
  public static final String recordDimName = "obs";
  public static final String latName = "latitude";
  public static final String lonName = "longitude";
  public static final String altName = "altitude";
  public static final String timeName = "time";

  public static final String stationStructName = "station";
  public static final String stationDimName = "station";
  public static final String stationIdName = "station_id";
  public static final String stationAltName = "stationAltitude";
  public static final String descName = "station_description";
  public static final String wmoName = "wmo_id";
  public static final String stationIndexName = "stationIndex";

  public static final String profileStructName = "profile";
  public static final String profileDimName = "profile";
  public static final String profileIdName = "profileId";
  public static final String numberOfObsName = "nobs";
  public static final String profileTimeName = "profileTime";

  public static final String trajStructName = "trajectory";
  public static final String trajDimName = "traj";
  public static final String trajIdName = "trajectoryId";

  public static final int idMissingValue = -9999;
  private static boolean debug;

  public static int writeFeatureCollection(FeatureDatasetPoint fdpoint, String fileOut,
      NetcdfFileWriter.Version version) throws IOException {
    return writeFeatureCollection(fdpoint, fileOut, new CFPointWriterConfig(version));
  }

  /**
   * Write a FeatureDatasetPoint to a netcd3/4 file.
   *
   * @param fdpoint the FeatureDatasetPoint; do first FeatureCollection contained within.
   * @param fileOut write to the is file
   * @param config configuration
   * @return count of number of pointFeatures written.
   */
  public static int writeFeatureCollection(FeatureDatasetPoint fdpoint, String fileOut, CFPointWriterConfig config)
      throws IOException {

    for (DsgFeatureCollection fc : fdpoint.getPointFeatureCollectionList()) {

      if (fc instanceof PointFeatureCollection) {
        return writePointFeatureCollection(fdpoint, (PointFeatureCollection) fc, fileOut, config);

      } else if (fc instanceof StationTimeSeriesFeatureCollection) {
        return writeStationFeatureCollection(fdpoint, (StationTimeSeriesFeatureCollection) fc, fileOut, config);

      } else if (fc instanceof ProfileFeatureCollection) {
        return writeProfileFeatureCollection(fdpoint, (ProfileFeatureCollection) fc, fileOut, config);

      } else if (fc instanceof TrajectoryFeatureCollection) {
        return writeTrajectoryFeatureCollection(fdpoint, (TrajectoryFeatureCollection) fc, fileOut, config);

      } else if (fc instanceof StationProfileFeatureCollection) {
        return writeStationProfileFeatureCollection(fdpoint, (StationProfileFeatureCollection) fc, fileOut, config);

      } else if (fc instanceof TrajectoryProfileFeatureCollection) {
        return writeTrajectoryProfileFeatureCollection(fdpoint, (TrajectoryProfileFeatureCollection) fc, fileOut,
            config);
      }
    }

    return 0;
  }


  private static int writePointFeatureCollection(FeatureDatasetPoint fdpoint, PointFeatureCollection pfc,
      String fileOut, CFPointWriterConfig config) throws IOException {

    try (WriterCFPointCollection pointWriter = new WriterCFPointCollection(fileOut, fdpoint.getGlobalAttributes(),
        fdpoint.getDataVariables(), pfc.getTimeUnit(), pfc.getAltUnits(), config)) {

      pointWriter.setExtraVariables(pfc.getExtraVariables());

      int count = 0;
      for (PointFeature pf : pfc) {
        if (count == 0)
          pointWriter.writeHeader(pf);

        pointWriter.writeRecord(pf, pf.getFeatureData());
        count++;
        if (debug && count % 100 == 0)
          System.out.printf("%d ", count);
        if (debug && count % 1000 == 0)
          System.out.printf("%n ");
      }

      pointWriter.finish();
      return count;
    }
  }

  private static int writeStationFeatureCollection(FeatureDatasetPoint dataset, StationTimeSeriesFeatureCollection fc,
      String fileOut, CFPointWriterConfig config) throws IOException {

    try (WriterCFStationCollection cfWriter = new WriterCFStationCollection(fileOut, dataset.getGlobalAttributes(),
        dataset.getDataVariables(), fc.getTimeUnit(), fc.getAltUnits(), config)) {

      cfWriter.setExtraVariables(fc.getExtraVariables());

      ucar.nc2.ft.PointFeatureCollection pfc = fc.flatten(null, null, null); // all data, but no need to sort by station

      int count = 0;
      for (PointFeature pf : pfc) {
        StationPointFeature spf = (StationPointFeature) pf;
        if (count == 0)
          cfWriter.writeHeader(fc.getStationFeatures(), spf);

        cfWriter.writeRecord(spf.getStation(), pf, pf.getFeatureData());
        count++;
        if (debug && count % 100 == 0)
          System.out.printf("%d ", count);
        if (debug && count % 1000 == 0)
          System.out.printf("%n ");
      }

      cfWriter.finish();
      return count;
    }
  }

  private static int writeProfileFeatureCollection(FeatureDatasetPoint fdpoint, ProfileFeatureCollection fc,
      String fileOut, CFPointWriterConfig config) throws IOException {

    try (WriterCFProfileCollection cfWriter = new WriterCFProfileCollection(fileOut, fdpoint.getGlobalAttributes(),
        fdpoint.getDataVariables(), fc.getTimeUnit(), fc.getAltUnits(), config)) {

      cfWriter.setExtraVariables(fc.getExtraVariables());

      // LOOK not always needed
      int count = 0;
      int name_strlen = 0;
      int nprofiles = fc.size();
      if (nprofiles < 0) {
        for (ProfileFeature pf : fc) {
          name_strlen = Math.max(name_strlen, pf.getName().length());
          count++;
        }
        nprofiles = count;
      }
      cfWriter.setFeatureAuxInfo(nprofiles, name_strlen);

      count = 0;
      for (ProfileFeature profile : fc) {
        count += cfWriter.writeProfile(profile);
        if (debug && count % 10 == 0)
          System.out.printf("%d ", count);
        if (debug && count % 100 == 0)
          System.out.printf("%n ");
      }

      cfWriter.finish();
      return count;
    }
  }

  private static int writeTrajectoryFeatureCollection(FeatureDatasetPoint fdpoint, TrajectoryFeatureCollection fc,
      String fileOut, CFPointWriterConfig config) throws IOException {

    try (WriterCFTrajectoryCollection cfWriter = new WriterCFTrajectoryCollection(fileOut,
        fdpoint.getGlobalAttributes(), fdpoint.getDataVariables(), fc.getTimeUnit(), fc.getAltUnits(), config)) {

      cfWriter.setExtraVariables(fc.getExtraVariables());

      // LOOK not always needed
      int count = 0;
      int name_strlen = 0;
      int ntrajs = fc.size();
      if (ntrajs < 0) {
        for (TrajectoryFeature traj : fc) {
          name_strlen = Math.max(name_strlen, traj.getName().length());
          count++;
        }
        ntrajs = count;
      }
      cfWriter.setFeatureAuxInfo(ntrajs, name_strlen);

      count = 0;
      for (TrajectoryFeature traj : fc) {
        count += cfWriter.writeTrajectory(traj);
        if (debug && count % 10 == 0)
          System.out.printf("%d ", count);
        if (debug && count % 100 == 0)
          System.out.printf("%n ");
      }

      cfWriter.finish();
      return count;
    }
  }

  private static int writeStationProfileFeatureCollection(FeatureDatasetPoint dataset,
      StationProfileFeatureCollection fc, String fileOut, CFPointWriterConfig config) throws IOException {

    try (WriterCFStationProfileCollection cfWriter = new WriterCFStationProfileCollection(fileOut,
        dataset.getGlobalAttributes(), dataset.getDataVariables(), fc.getTimeUnit(), fc.getAltUnits(), config)) {

      cfWriter.setExtraVariables(fc.getExtraVariables());
      cfWriter.setStations(fc.getStationFeatures());

      int name_strlen = 0;
      int countProfiles = 0;
      for (StationProfileFeature spf : fc) {
        name_strlen = Math.max(name_strlen, spf.getName().length());
        if (spf.size() >= 0)
          countProfiles += spf.size();
        else {
          for (ProfileFeature pf : spf) {
            countProfiles++;
          }
        }
      }
      cfWriter.setFeatureAuxInfo(countProfiles, name_strlen);

      int count = 0;
      for (StationProfileFeature spf : fc) {
        for (ProfileFeature pf : spf) {
          if (pf.getTime() == null)
            continue; // assume this means its an "incomplete multidimensional"

          count += cfWriter.writeProfile(spf, pf);
          if (debug && count % 100 == 0)
            System.out.printf("%d ", count);
          if (debug && count % 1000 == 0)
            System.out.printf("%n ");
        }
      }

      cfWriter.finish();
      return count;
    }
  }

  private static int writeTrajectoryProfileFeatureCollection(FeatureDatasetPoint dataset,
      TrajectoryProfileFeatureCollection fc, String fileOut, CFPointWriterConfig config) throws IOException {

    try (WriterCFTrajectoryProfileCollection cfWriter = new WriterCFTrajectoryProfileCollection(fileOut,
        dataset.getGlobalAttributes(), dataset.getDataVariables(), fc.getTimeUnit(), fc.getAltUnits(), config)) {

      cfWriter.setExtraVariables(fc.getExtraVariables());

      int traj_strlen = 0;
      int prof_strlen = 0;
      int countTrajectories = 0;
      int countProfiles = 0;
      for (TrajectoryProfileFeature spf : fc) {
        countTrajectories++;
        traj_strlen = Math.max(traj_strlen, spf.getName().length());
        if (spf.size() >= 0)
          countProfiles += spf.size();
        else {
          for (ProfileFeature profile : spf) {
            prof_strlen = Math.max(prof_strlen, profile.getName().length());
            countProfiles++;
          }
        }
      }
      cfWriter.setFeatureAuxInfo(countProfiles, prof_strlen);
      cfWriter.setFeatureAuxInfo2(countTrajectories, traj_strlen);

      int count = 0;
      for (TrajectoryProfileFeature spf : fc) {
        for (ProfileFeature profile : spf) {
          if (profile.getTime() == null)
            continue; // assume this means its a "incomplete multidimensional"

          count += cfWriter.writeProfile(spf, profile);
          if (debug && count % 100 == 0)
            System.out.printf("%d ", count);
          if (debug && count % 1000 == 0)
            System.out.printf("%n ");
        }
      }

      cfWriter.finish();
      return count;
    }
  }


  //////////////////////////////////////////////////////////////////////////////////////////////////

  // attributes with these names will not be copied to the output file
  protected static final List<String> reservedGlobalAtts =
      Arrays.asList(CDM.CONVENTIONS, ACDD.LAT_MIN, ACDD.LAT_MAX, ACDD.LON_MIN, ACDD.LON_MAX, ACDD.TIME_START,
          ACDD.TIME_END, _Coordinate._CoordSysBuilder, CF.featureTypeAtt2, CF.featureTypeAtt3);

  protected static final List<String> reservedVariableAtts = Arrays.asList(CF.SAMPLE_DIMENSION, CF.INSTANCE_DIMENSION);

  /////////////////////////////////////////////////
  protected final CFPointWriterConfig config;
  protected NetcdfFileWriter writer;

  protected CalendarDateUnit timeUnit;
  protected String altUnits;
  protected String altitudeCoordinateName = altName;

  protected final boolean noTimeCoverage;
  protected final boolean noUnlimitedDimension; // experimental , netcdf-3
  protected final boolean isExtendedModel;
  protected boolean useAlt = true;
  protected int nfeatures, id_strlen;

  private Map<String, Dimension> dimMap = new HashMap<>();
  protected Structure record; // used for netcdf3 and netcdf4 extended
  protected Dimension recordDim;
  protected Map<String, Variable> dataMap = new HashMap<>();
  protected List<VariableSimpleIF> dataVars;

  private Map<String, Variable> extraMap; // added as variables just as they are
  protected List<Variable> extra;

  protected LatLonRect llbb;
  protected CalendarDate minDate;
  protected CalendarDate maxDate;

  // LOOK doesnt work
  protected CFPointWriter(String fileOut, List<Attribute> atts, NetcdfFileWriter.Version version) throws IOException {
    this(fileOut, atts, null, null, null, new CFPointWriterConfig(version));
  }

  /**
   * Ctor
   * 
   * @param fileOut name of the output file
   * @param atts global attributes to be added
   * @param config configure
   */
  protected CFPointWriter(String fileOut, List<Attribute> atts, List<VariableSimpleIF> dataVars,
      CalendarDateUnit timeUnit, String altUnits, CFPointWriterConfig config) throws IOException {
    createWriter(fileOut, config);
    this.dataVars = dataVars;
    this.timeUnit = timeUnit;
    this.altUnits = altUnits;
    this.config = config;
    this.noTimeCoverage = config.noTimeCoverage;
    this.noUnlimitedDimension =
        (writer.getVersion() == NetcdfFileWriter.Version.netcdf3) && config.recDimensionLength >= 0; // LOOK NOT USED
    this.isExtendedModel = writer.getVersion().isExtendedModel();

    addGlobalAtts(atts);
    addNetcdf3UnknownAtts(noTimeCoverage);
  }


  public void setFeatureAuxInfo(int nfeatures, int id_strlen) {
    this.nfeatures = nfeatures;
    this.id_strlen = id_strlen;
  }

  protected VariableSimpleIF getDataVar(String name) {
    for (VariableSimpleIF v : dataVars)
      if (v.getShortName().equals(name))
        return v;
    return null;
  }

  private void createWriter(String fileOut, CFPointWriterConfig config) throws IOException {
    writer = NetcdfFileWriter.createNew(config.version, fileOut, config.chunking);
    writer.setFill(false);
  }

  private void addGlobalAtts(List<Attribute> atts) {
    writer.addGroupAttribute(null, new Attribute(CDM.CONVENTIONS, isExtendedModel ? CDM.CF_EXTENDED : "CF-1.6"));
    writer.addGroupAttribute(null, new Attribute(CDM.HISTORY, "Written by CFPointWriter"));
    for (Attribute att : atts) {
      if (!reservedGlobalAtts.contains(att.getShortName()))
        writer.addGroupAttribute(null, att);
    }
  }

  // netcdf3 has to add attributes up front, but we dont know values until the end.
  // so we have this updateAttribute hack; values set in finish()
  private void addNetcdf3UnknownAtts(boolean noTimeCoverage) {
    // dummy values, update in finish()
    if (!noTimeCoverage) {
      CalendarDate now = CalendarDate.of(new Date());
      writer.addGroupAttribute(null, new Attribute(ACDD.TIME_START, CalendarDateFormatter.toDateTimeStringISO(now)));
      writer.addGroupAttribute(null, new Attribute(ACDD.TIME_END, CalendarDateFormatter.toDateTimeStringISO(now)));
    }
    writer.addGroupAttribute(null, new Attribute(ACDD.LAT_MIN, 0.0));
    writer.addGroupAttribute(null, new Attribute(ACDD.LAT_MAX, 0.0));
    writer.addGroupAttribute(null, new Attribute(ACDD.LON_MIN, 0.0));
    writer.addGroupAttribute(null, new Attribute(ACDD.LON_MAX, 0.0));
  }

  void setExtraVariables(List<Variable> extra) {
    this.extra = extra;
    if (extra != null) {
      for (Variable v : extra) {
        if (v instanceof CoordinateAxis) {
          CoordinateAxis axis = (CoordinateAxis) v;
          if (axis.getAxisType() == AxisType.Height) {
            useAlt = false; // dont need another altitude variable
            altitudeCoordinateName = v.getFullName();
          }
        }
      }
    }
  }

  protected abstract void makeFeatureVariables(StructureData featureData, boolean isExtended);

  protected void makeMiddleVariables(StructureData middleData, boolean isExtended) {
    // NOOP
  }

  protected void writeHeader(List<VariableSimpleIF> obsCoords, StructureData featureData, StructureData obsData,
      String coordNames) throws IOException {
    this.recordDim = writer.addUnlimitedDimension(recordDimName);

    addExtraVariables();

    if (writer.getVersion().isExtendedModel()) {
      makeFeatureVariables(featureData, true);
      record = (Structure) writer.addVariable(null, recordName, DataType.STRUCTURE, recordDimName);
      addCoordinatesExtended(record, obsCoords);
      addDataVariablesExtended(obsData, coordNames);
      writer.create();

    } else {
      makeFeatureVariables(featureData, false);
      addCoordinatesClassic(recordDim, obsCoords, dataMap);
      addDataVariablesClassic(recordDim, obsData, dataMap, coordNames);
      writer.create();
      record = writer.addRecordStructure(); // for netcdf3
    }

    writeExtraVariables();
  }

  protected void writeHeader2(List<VariableSimpleIF> obsCoords, StructureData featureData, StructureData middleData,
      StructureData obsData, String coordNames) throws IOException {
    this.recordDim = writer.addUnlimitedDimension(recordDimName);

    addExtraVariables();

    if (writer.getVersion().isExtendedModel()) {
      makeFeatureVariables(featureData, true);
      makeMiddleVariables(middleData, true);
      record = (Structure) writer.addVariable(null, recordName, DataType.STRUCTURE, recordDimName);
      addCoordinatesExtended(record, obsCoords);
      addDataVariablesExtended(obsData, coordNames);
      writer.create();

    } else {
      makeFeatureVariables(featureData, false);
      makeMiddleVariables(middleData, false);
      addCoordinatesClassic(recordDim, obsCoords, dataMap);
      addDataVariablesClassic(recordDim, obsData, dataMap, coordNames);
      writer.create();
      record = writer.addRecordStructure(); // for netcdf3
    }

    writeExtraVariables();
  }

  protected void addExtraVariables() {
    if (extra == null)
      return;
    if (extraMap == null)
      extraMap = new HashMap<>();

    addDimensionsClassic(extra, dimMap);

    for (VariableSimpleIF vs : extra) {
      List<Dimension> dims = makeDimensionList(dimMap, vs.getDimensions());
      Variable mv = writer.addVariable(null, vs.getShortName(), vs.getDataType(), dims);
      for (Attribute att : vs.attributes())
        mv.addAttribute(att);
      extraMap.put(mv.getShortName(), mv);
    }
  }

  protected void writeExtraVariables() throws IOException {
    if (extra == null)
      return;

    for (Variable v : extra) {
      Variable mv = extraMap.get(v.getShortName());
      if (mv == null)
        continue; // may be removed
      try {
        writer.write(mv, v.read());
      } catch (InvalidRangeException e) {
        e.printStackTrace(); // cant happen haha
      }
    }
  }

  // added as variables with the unlimited (record) dimension
  protected void addCoordinatesClassic(Dimension recordDim, List<VariableSimpleIF> coords,
      Map<String, Variable> varMap) {
    addDimensionsClassic(coords, dimMap);

    for (VariableSimpleIF oldVar : coords) {
      List<Dimension> dims = makeDimensionList(dimMap, oldVar.getDimensions());
      dims.add(0, recordDim);
      Variable newVar;
      if (oldVar.getDataType() == DataType.STRING && !writer.getVersion().isExtendedModel()) {
        if (oldVar instanceof Variable)
          newVar = writer.addStringVariable(null, (Variable) oldVar, dims);
        else
          newVar = writer.addStringVariable(null, oldVar.getShortName(), dims, 20); // LOOK barf
      } else {
        newVar = writer.addVariable(null, oldVar.getShortName(), oldVar.getDataType(), dims);
      }

      if (newVar == null) {
        logger.warn("Variable already exists =" + oldVar.getShortName());
        continue;
      }

      for (Attribute att : oldVar.attributes())
        newVar.addAttribute(att);
      varMap.put(newVar.getShortName(), newVar);
    }

  }

  // added as members of the given structure
  protected void addCoordinatesExtended(Structure parent, List<VariableSimpleIF> coords) {
    for (VariableSimpleIF vs : coords) {
      String dims = Dimensions.makeDimensionsString(vs.getDimensions());
      Variable member = writer.addStructureMember(parent, vs.getShortName(), vs.getDataType(), dims);

      if (member == null) {
        logger.warn("Variable already exists =" + vs.getShortName()); // LOOK barf
        continue;
      }

      for (Attribute att : vs.attributes())
        member.addAttribute(att);
    }
  }

  // added as variables with the unlimited (record) dimension
  protected void addDataVariablesClassic(Dimension recordDim, StructureData stnData, Map<String, Variable> varMap,
      String coordVars) {
    addDimensionsClassic(dataVars, dimMap);

    for (StructureMembers.Member m : stnData.getMembers()) {
      VariableSimpleIF oldVar = getDataVar(m.getName());
      if (oldVar == null)
        continue;

      List<Dimension> dims = makeDimensionList(dimMap, oldVar.getDimensions());
      dims.add(0, recordDim);

      Variable newVar;
      if (oldVar.getDataType() == DataType.STRING && !writer.getVersion().isExtendedModel()) {
        if (oldVar instanceof Variable)
          newVar = writer.addStringVariable(null, (Variable) oldVar, dims);
        else
          newVar = writer.addStringVariable(null, oldVar.getShortName(), dims, 20); // LOOK barf

      } else {
        VariableSimpleIF prevVar = writer.findVariable(oldVar.getShortName());
        if (prevVar != null) {
          if (extraMap.get(oldVar.getShortName()) != null) { // this is normal, extra got added but not actually needed
            writer.deleteVariable(oldVar.getShortName());
            extraMap.remove(oldVar.getShortName());
          }
        }
        newVar = writer.addVariable(null, oldVar.getShortName(), oldVar.getDataType(), dims);
        if (newVar == null) {
          logger.warn("Variable already exists =" + oldVar.getShortName()); // LOOK WHY?
          continue;
        }
      }

      for (Attribute att : oldVar.attributes()) {
        String attName = att.getShortName();
        if (!reservedVariableAtts.contains(attName) && !attName.startsWith("_Coordinate"))
          newVar.addAttribute(att);
      }

      newVar.addAttribute(new Attribute(CF.COORDINATES, coordVars));

      varMap.put(newVar.getShortName(), newVar);
    }

  }

  // add variables to the record structure
  protected void addDataVariablesExtended(StructureData obsData, String coordVars) {

    for (StructureMembers.Member m : obsData.getMembers()) {
      VariableSimpleIF oldVar = getDataVar(m.getName());
      if (oldVar == null)
        continue;

      // skip duplicates
      // if (record.findVariable(oldVar.getShortName()) != null) continue;

      // make dimension list
      StringBuilder dimNames = new StringBuilder();
      for (Dimension d : oldVar.getDimensions()) {
        if (d.isUnlimited())
          continue;
        if (d.getShortName() == null || !d.getShortName().equals(recordDimName))
          dimNames.append(" ").append(d.getLength()); // anonymous
      }

      Variable newVar =
          writer.addStructureMember(record, oldVar.getShortName(), oldVar.getDataType(), dimNames.toString());
      if (newVar == null) {
        logger.warn("Variable already exists =" + oldVar.getShortName()); // LOOK barf
        continue;
      }

      for (Attribute att : oldVar.attributes()) {
        String attName = att.getShortName();
        if (!reservedVariableAtts.contains(attName) && !attName.startsWith("_Coordinate"))
          newVar.addAttribute(att);
      }
      newVar.addAttribute(new Attribute(CF.COORDINATES, coordVars));
    }

  }

  // classic model: no private dimensions
  protected void addDimensionsClassic(List<? extends VariableSimpleIF> vars, Map<String, Dimension> dimMap) {
    Set<Dimension> oldDims = new HashSet<>(20);

    // find all dimensions needed by these variables
    for (VariableSimpleIF var : vars) {
      List<Dimension> dims = var.getDimensions();
      oldDims.addAll(dims);
    }

    // add them
    for (Dimension d : oldDims) {
      // The dimension we're creating below will be shared, so we need an appropriate name for it.
      String dimName = getSharedDimName(d);

      if (!writer.hasDimension(null, dimName)) {
        Dimension newDim = writer.addDimension(null, dimName, d.getLength(), false, d.isVariableLength());
        dimMap.put(dimName, newDim);
      }
    }
  }

  protected List<Dimension> makeDimensionList(Map<String, Dimension> dimMap, List<Dimension> oldDims) {
    List<Dimension> result = new ArrayList<>();

    // find all dimensions needed by the coord variables
    for (Dimension dim : oldDims) {
      Dimension newDim = dimMap.get(getSharedDimName(dim));
      assert newDim != null : "Oops, we screwed up: dimMap doesn't contain " + getSharedDimName(dim);
      result.add(newDim);
    }

    return result;
  }

  /**
   * Returns a name for {@code dim} that is suitable for a shared dimension. If the dimension is anonymous, meaning
   * that its name is {@code null}, we return a default name: {@code "len" + dim.getLength()}. Otherwise, we return the
   * dimension's existing name.
   *
   * @param dim a dimension.
   * @return a name that is suitable for a shared dimension, i.e. not {@code null}.
   */
  public static String getSharedDimName(Dimension dim) {
    if (dim.getShortName() == null) { // Dim is anonymous.
      return "len" + dim.getLength();
    } else {
      return dim.getShortName();
    }
  }

  protected int writeStructureData(int recno, Structure s, StructureData sdata, Map<String, Variable> varMap)
      throws IOException {

    // write the recno record
    int[] origin = new int[1];
    origin[0] = recno;
    try {
      if (isExtendedModel) {
        if (s.isUnlimited())
          return writer.appendStructureData(s, sdata); // can write it all at once along unlimited dimension
        else {
          ArrayStructureW as = new ArrayStructureW(sdata.getStructureMembers(), new int[] {1});
          as.setStructureData(sdata, 0);
          writer.write(s, origin, as); // can write it all at once along regular dimension
          return recno + 1;
        }

      } else {
        writeStructureDataClassic(varMap, origin, sdata);
      }

    } catch (InvalidRangeException e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }

    return recno + 1;
  }

  protected int writeStructureDataClassic(Map<String, Variable> varMap, int[] origin, StructureData sdata)
      throws IOException, InvalidRangeException {
    for (StructureMembers.Member m : sdata.getMembers()) {
      Variable mv = varMap.get(m.getName());
      if (mv == null)
        continue; // ok

      Array org = sdata.getArray(m);
      if (m.getDataType() == DataType.STRING) { // convert to ArrayChar
        int strlen = mv.getDimension(mv.getDimensions().size() - 1).getLength();
        org = ArrayChar.makeFromStringArray((ArrayObject) org, strlen);
      }

      Array orgPlus1 = Array.makeArrayRankPlusOne(org); // add dimension on the left (slow)
      int[] useOrigin = origin;

      if (org.getRank() > 0) { // if rank 0 (common case, this is a nop, so skip
        useOrigin = new int[org.getRank() + 1];
        useOrigin[0] = origin[0]; // the rest are 0
      }

      writer.write(mv, useOrigin, orgPlus1);
    }

    return origin[0];
  }


  // keep track of the bounding box
  protected void trackBB(LatLonPoint loc, CalendarDate obsDate) {
    if (loc != null) {
      if (llbb == null) {
        llbb = new LatLonRect(loc, .001, .001);
      } else {
        llbb.extend(loc);
      }
    }

    // date is handled specially
    if ((minDate == null) || minDate.isAfter(obsDate))
      minDate = obsDate;
    if ((maxDate == null) || maxDate.isBefore(obsDate))
      maxDate = obsDate;
  }

  public void finish() throws IOException {
    if (llbb != null) {
      writer.updateAttribute(null, new Attribute(ACDD.LAT_MIN, llbb.getLowerLeftPoint().getLatitude()));
      writer.updateAttribute(null, new Attribute(ACDD.LAT_MAX, llbb.getUpperRightPoint().getLatitude()));
      writer.updateAttribute(null, new Attribute(ACDD.LON_MIN, llbb.getLowerLeftPoint().getLongitude()));
      writer.updateAttribute(null, new Attribute(ACDD.LON_MAX, llbb.getUpperRightPoint().getLongitude()));
    }

    if (!noTimeCoverage) {
      if (minDate == null)
        minDate = CalendarDate.present();
      if (maxDate == null)
        maxDate = CalendarDate.present();
      writer.updateAttribute(null, new Attribute(ACDD.TIME_START, CalendarDateFormatter.toDateTimeStringISO(minDate)));
      writer.updateAttribute(null, new Attribute(ACDD.TIME_END, CalendarDateFormatter.toDateTimeStringISO(maxDate)));
    }

    writer.close();
  }

  @Override
  public void close() throws IOException {
    writer.close();
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////

  private static class CommandLine {
    @Parameter(names = {"-i", "--input"}, description = "Input file.", required = true)
    public File inputFile;

    @Parameter(names = {"-o", "--output"}, description = "Output file.", required = true)
    public File outputFile;

    @Parameter(names = {"-f", "--format"}, description = "Output file format. Allowed values = "
        + "[netcdf3, netcdf4, netcdf4_classic, netcdf3c, netcdf3c64, ncstream]")
    public NetcdfFileWriter.Version format = NetcdfFileWriter.Version.netcdf3;

    @Parameter(names = {"-st", "--strategy"},
        description = "Chunking strategy. Only used in NetCDF 4. " + "Allowed values = [standard, grib, none]")
    public Nc4Chunking.Strategy strategy = Nc4Chunking.Strategy.standard;

    @Parameter(names = {"-d", "--deflateLevel"}, description = "Compression level. Only used in NetCDF 4. "
        + "Allowed values = 0 (no compression, fast) to 9 (max compression, slow)")
    public int deflateLevel = 5;

    @Parameter(names = {"-sh", "--shuffle"}, description = "Enable the shuffle filter, which may improve compression. "
        + "Only used in NetCDF 4. This option is ignored unless a non-zero deflate level is specified.")
    public boolean shuffle = true;

    @Parameter(names = {"-h", "--help"}, description = "Display this help and exit", help = true)
    public boolean help;


    private static class ParameterDescriptionComparator implements Comparator<ParameterDescription> {
      // Display parameters in this order in the usage information.
      private final List<String> orderedParamNames =
          Arrays.asList("--input", "--output", "--format", "--strategy", "--deflateLevel", "--shuffle", "--help");

      @Override
      public int compare(ParameterDescription p0, ParameterDescription p1) {
        int index0 = orderedParamNames.indexOf(p0.getLongestName());
        int index1 = orderedParamNames.indexOf(p1.getLongestName());
        assert index0 >= 0 : "Unexpected parameter name: " + p0.getLongestName();
        assert index1 >= 0 : "Unexpected parameter name: " + p1.getLongestName();

        return Integer.compare(index0, index1);
      }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    private final JCommander jc;

    public CommandLine(String progName, String[] args) throws ParameterException {
      this.jc = new JCommander(this, args); // Parses args and uses them to initialize *this*.
      jc.setProgramName(progName); // Displayed in the usage information.

      // Set the ordering of of parameters in the usage information.
      jc.setParameterDescriptionComparator(new ParameterDescriptionComparator());
    }

    public void printUsage() {
      jc.usage();
    }

    public Nc4Chunking getNc4Chunking() {
      return Nc4ChunkingStrategy.factory(strategy, deflateLevel, shuffle);
    }

    public CFPointWriterConfig getCFPointWriterConfig() {
      return new CFPointWriterConfig(format, getNc4Chunking());
    }
  }

  public static void main(String[] args) throws Exception {
    String progName = CFPointWriter.class.getName();

    try {
      CommandLine cmdLine = new CommandLine(progName, args);

      if (cmdLine.help) {
        cmdLine.printUsage();
        return;
      }

      FeatureType wantFeatureType = FeatureType.ANY_POINT;
      String location = cmdLine.inputFile.getAbsolutePath();
      CancelTask cancel = null;
      Formatter errlog = new Formatter();

      try (FeatureDatasetPoint fdPoint =
          (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(wantFeatureType, location, cancel, errlog)) {
        if (fdPoint == null) {
          System.err.println(errlog);
        } else {
          System.out.printf("CFPointWriter: reading from %s, writing to %s%n", cmdLine.inputFile, cmdLine.outputFile);
          writeFeatureCollection(fdPoint, cmdLine.outputFile.getAbsolutePath(), cmdLine.getCFPointWriterConfig());
          System.out.println("Done.");
        }
      }
    } catch (ParameterException e) {
      System.err.println(e.getMessage());
      System.err.printf("Try \"%s --help\" for more information.%n", progName);
    }
  }
}
