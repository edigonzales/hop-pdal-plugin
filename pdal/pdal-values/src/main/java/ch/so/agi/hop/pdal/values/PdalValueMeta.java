package ch.so.agi.hop.pdal.values;

import ch.so.agi.hop.pointcloud.type.ValueMetaPointCloud;
import java.util.List;
import java.util.Locale;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.core.row.value.ValueMetaInteger;
import org.apache.hop.core.row.value.ValueMetaNumber;
import org.apache.hop.core.row.value.ValueMetaString;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.apache.hop.metadata.api.IHopMetadataProvider;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.BaseTransformMeta;
import org.apache.hop.pipeline.transform.ITransform;
import org.apache.hop.pipeline.transform.ITransformData;
import org.apache.hop.pipeline.transform.ITransformMeta;
import org.apache.hop.pipeline.transform.TransformMeta;

/**
 * Shared configuration of the point cloud transforms.
 *
 * <p>One meta class per operation only declares the operation; all properties live here so the
 * dialog and serialization stay in one place. The Hop metadata property names are stable and
 * versioned through {@code version}.
 */
public abstract class PdalValueMeta extends BaseTransformMeta<PdalValueTransform, PdalValueData> {

  public enum Operation {
    READER,
    INFO,
    CROP,
    REPROJECT,
    WRITER,
    FILTER,
    RANGE,
    CALCULATOR,
    CLASSIFICATION,
    THIN,
    OUTLIER,
    SORT,
    TRANSFORM,
    GROUND,
    RAW,
    STATISTICS,
    MERGER,
    TO_ROWS
  }

  public abstract Operation operation();

  @HopMetadataProperty private int version = 1;

  public int getVersion() {
    return version;
  }

  public void setVersion(int value) {
    version = value;
  }

  @HopMetadataProperty private String valueField = "pointcloud";

  public String getValueField() {
    return valueField;
  }

  public void setValueField(String value) {
    valueField = value;
  }

  @HopMetadataProperty private String outputValueField = "";

  public String getOutputValueField() {
    return outputValueField;
  }

  public void setOutputValueField(String value) {
    outputValueField = value;
  }

  @HopMetadataProperty private String prefix = "";

  public String getPrefix() {
    return prefix;
  }

  public void setPrefix(String value) {
    prefix = value;
  }

  @HopMetadataProperty private String where = "";

  public String getWhere() {
    return where;
  }

  public void setWhere(String value) {
    where = value;
  }

  // ----- reader -----

  @HopMetadataProperty private String source = "";

  public String getSource() {
    return source;
  }

  public void setSource(String value) {
    source = value;
  }

  @HopMetadataProperty private boolean sourceField = false;

  public boolean isSourceField() {
    return sourceField;
  }

  public void setSourceField(boolean value) {
    sourceField = value;
  }

  @HopMetadataProperty private String driver = "";

  public String getDriver() {
    return driver;
  }

  public void setDriver(String value) {
    driver = value;
  }

  @HopMetadataProperty private String overrideSrs = "";

  public String getOverrideSrs() {
    return overrideSrs;
  }

  public void setOverrideSrs(String value) {
    overrideSrs = value;
  }

  @HopMetadataProperty private String resolution = "";

  public String getResolution() {
    return resolution;
  }

  public void setResolution(String value) {
    resolution = value;
  }

  // ----- writer -----

  @HopMetadataProperty private boolean overwrite = false;

  public boolean isOverwrite() {
    return overwrite;
  }

  public void setOverwrite(boolean value) {
    overwrite = value;
  }

  @HopMetadataProperty private String compression = "";

  public String getCompression() {
    return compression;
  }

  public void setCompression(String value) {
    compression = value;
  }

  @HopMetadataProperty private String minorVersion = "";

  public String getMinorVersion() {
    return minorVersion;
  }

  public void setMinorVersion(String value) {
    minorVersion = value;
  }

  @HopMetadataProperty private String extraDims = "";

  public String getExtraDims() {
    return extraDims;
  }

  public void setExtraDims(String value) {
    extraDims = value;
  }

  @HopMetadataProperty private String threads = "";

  public String getThreads() {
    return threads;
  }

  public void setThreads(String value) {
    threads = value;
  }

  @HopMetadataProperty private String forward = "";

  public String getForward() {
    return forward;
  }

  public void setForward(String value) {
    forward = value;
  }

  // ----- crop -----

  @HopMetadataProperty private String minX = "";

  public String getMinX() {
    return minX;
  }

  public void setMinX(String value) {
    minX = value;
  }

  @HopMetadataProperty private String minY = "";

  public String getMinY() {
    return minY;
  }

  public void setMinY(String value) {
    minY = value;
  }

  @HopMetadataProperty private String minZ = "";

  public String getMinZ() {
    return minZ;
  }

  public void setMinZ(String value) {
    minZ = value;
  }

  @HopMetadataProperty private String maxX = "";

  public String getMaxX() {
    return maxX;
  }

  public void setMaxX(String value) {
    maxX = value;
  }

  @HopMetadataProperty private String maxY = "";

  public String getMaxY() {
    return maxY;
  }

  public void setMaxY(String value) {
    maxY = value;
  }

  @HopMetadataProperty private String maxZ = "";

  public String getMaxZ() {
    return maxZ;
  }

  public void setMaxZ(String value) {
    maxZ = value;
  }

  @HopMetadataProperty private boolean boundsFields = false;

  public boolean isBoundsFields() {
    return boundsFields;
  }

  public void setBoundsFields(boolean value) {
    boundsFields = value;
  }

  // ----- reproject -----

  @HopMetadataProperty private String targetCrs = "";

  public String getTargetCrs() {
    return targetCrs;
  }

  public void setTargetCrs(String value) {
    targetCrs = value;
  }

  @HopMetadataProperty private boolean targetCrsField = false;

  public boolean isTargetCrsField() {
    return targetCrsField;
  }

  public void setTargetCrsField(boolean value) {
    targetCrsField = value;
  }

  // ----- info -----

  @HopMetadataProperty private String infoFields = "point_count,bounds,crs,dimensions";

  public String getInfoFields() {
    return infoFields;
  }

  public void setInfoFields(String value) {
    infoFields = value;
  }

  // ----- filter -----

  @HopMetadataProperty private String expression = "";

  public String getExpression() {
    return expression;
  }

  public void setExpression(String value) {
    expression = value;
  }

  @HopMetadataProperty private boolean invertFilter = false;

  public boolean isInvertFilter() {
    return invertFilter;
  }

  public void setInvertFilter(boolean value) {
    invertFilter = value;
  }

  @HopMetadataProperty private String limits = "";

  public String getLimits() {
    return limits;
  }

  public void setLimits(String value) {
    limits = value;
  }

  // ----- calculator / classification -----

  @HopMetadataProperty private String assignmentKind = "VALUE";

  public String getAssignmentKind() {
    return assignmentKind;
  }

  public void setAssignmentKind(String value) {
    assignmentKind = value;
  }

  @HopMetadataProperty private String assignment = "";

  public String getAssignment() {
    return assignment;
  }

  public void setAssignment(String value) {
    assignment = value;
  }

  @HopMetadataProperty private String condition = "";

  public String getCondition() {
    return condition;
  }

  public void setCondition(String value) {
    condition = value;
  }

  @HopMetadataProperty private String classification = "";

  public String getClassification() {
    return classification;
  }

  public void setClassification(String value) {
    classification = value;
  }

  @HopMetadataProperty private boolean onlyUnclassified = false;

  public boolean isOnlyUnclassified() {
    return onlyUnclassified;
  }

  public void setOnlyUnclassified(boolean value) {
    onlyUnclassified = value;
  }

  // ----- thin -----

  @HopMetadataProperty private String thinType = "DECIMATION";

  public String getThinType() {
    return thinType;
  }

  public void setThinType(String value) {
    thinType = value;
  }

  @HopMetadataProperty private String step = "";

  public String getStep() {
    return step;
  }

  public void setStep(String value) {
    step = value;
  }

  @HopMetadataProperty private String voxelCell = "";

  public String getVoxelCell() {
    return voxelCell;
  }

  public void setVoxelCell(String value) {
    voxelCell = value;
  }

  @HopMetadataProperty private String voxelMode = "first";

  public String getVoxelMode() {
    return voxelMode;
  }

  public void setVoxelMode(String value) {
    voxelMode = value;
  }

  @HopMetadataProperty private String gridCell = "";

  public String getGridCell() {
    return gridCell;
  }

  public void setGridCell(String value) {
    gridCell = value;
  }

  @HopMetadataProperty private String sampleRadius = "";

  public String getSampleRadius() {
    return sampleRadius;
  }

  public void setSampleRadius(String value) {
    sampleRadius = value;
  }

  @HopMetadataProperty private String fpsCount = "";

  public String getFpsCount() {
    return fpsCount;
  }

  public void setFpsCount(String value) {
    fpsCount = value;
  }

  // ----- outlier -----

  @HopMetadataProperty private String outlierType = "STATISTICAL";

  public String getOutlierType() {
    return outlierType;
  }

  public void setOutlierType(String value) {
    outlierType = value;
  }

  @HopMetadataProperty private String meanK = "8";

  public String getMeanK() {
    return meanK;
  }

  public void setMeanK(String value) {
    meanK = value;
  }

  @HopMetadataProperty private String multiplier = "2.0";

  public String getMultiplier() {
    return multiplier;
  }

  public void setMultiplier(String value) {
    multiplier = value;
  }

  @HopMetadataProperty private String radius = "";

  public String getRadius() {
    return radius;
  }

  public void setRadius(String value) {
    radius = value;
  }

  @HopMetadataProperty private String minK = "2";

  public String getMinK() {
    return minK;
  }

  public void setMinK(String value) {
    minK = value;
  }

  // ----- sort -----

  @HopMetadataProperty private String sortDimensions = "";

  public String getSortDimensions() {
    return sortDimensions;
  }

  public void setSortDimensions(String value) {
    sortDimensions = value;
  }

  @HopMetadataProperty private boolean sortDescending = false;

  public boolean isSortDescending() {
    return sortDescending;
  }

  public void setSortDescending(boolean value) {
    sortDescending = value;
  }

  @HopMetadataProperty private boolean mortonOrder = false;

  public boolean isMortonOrder() {
    return mortonOrder;
  }

  public void setMortonOrder(boolean value) {
    mortonOrder = value;
  }

  @HopMetadataProperty private boolean mortonReverse = false;

  public boolean isMortonReverse() {
    return mortonReverse;
  }

  public void setMortonReverse(boolean value) {
    mortonReverse = value;
  }

  // ----- transform -----

  @HopMetadataProperty private String matrix = "";

  public String getMatrix() {
    return matrix;
  }

  public void setMatrix(String value) {
    matrix = value;
  }

  // ----- ground -----

  @HopMetadataProperty private String groundType = "SMRF";

  public String getGroundType() {
    return groundType;
  }

  public void setGroundType(String value) {
    groundType = value;
  }

  @HopMetadataProperty private String scalar = "1.25";

  public String getScalar() {
    return scalar;
  }

  public void setScalar(String value) {
    scalar = value;
  }

  @HopMetadataProperty private String slope = "0.15";

  public String getSlope() {
    return slope;
  }

  public void setSlope(String value) {
    slope = value;
  }

  @HopMetadataProperty private String threshold = "0.5";

  public String getThreshold() {
    return threshold;
  }

  public void setThreshold(String value) {
    threshold = value;
  }

  @HopMetadataProperty private String window = "18";

  public String getWindow() {
    return window;
  }

  public void setWindow(String value) {
    window = value;
  }

  @HopMetadataProperty private String groundCell = "";

  public String getGroundCell() {
    return groundCell;
  }

  public void setGroundCell(String value) {
    groundCell = value;
  }

  @HopMetadataProperty private String pmfCellSize = "1.0";

  public String getPmfCellSize() {
    return pmfCellSize;
  }

  public void setPmfCellSize(String value) {
    pmfCellSize = value;
  }

  @HopMetadataProperty private String initialDistance = "0.15";

  public String getInitialDistance() {
    return initialDistance;
  }

  public void setInitialDistance(String value) {
    initialDistance = value;
  }

  @HopMetadataProperty private String maxWindowSize = "33";

  public String getMaxWindowSize() {
    return maxWindowSize;
  }

  public void setMaxWindowSize(String value) {
    maxWindowSize = value;
  }

  @HopMetadataProperty private String csfResolution = "1.0";

  public String getCsfResolution() {
    return csfResolution;
  }

  public void setCsfResolution(String value) {
    csfResolution = value;
  }

  @HopMetadataProperty private String csfRigidness = "1";

  public String getCsfRigidness() {
    return csfRigidness;
  }

  public void setCsfRigidness(String value) {
    csfRigidness = value;
  }

  @HopMetadataProperty private boolean computeHag = false;

  public boolean isComputeHag() {
    return computeHag;
  }

  public void setComputeHag(boolean value) {
    computeHag = value;
  }

  @HopMetadataProperty private String hagType = "NN";

  public String getHagType() {
    return hagType;
  }

  public void setHagType(String value) {
    hagType = value;
  }

  @HopMetadataProperty private String hagDemFile = "";

  public String getHagDemFile() {
    return hagDemFile;
  }

  public void setHagDemFile(String value) {
    hagDemFile = value;
  }

  @HopMetadataProperty private String hagCount = "";

  public String getHagCount() {
    return hagCount;
  }

  public void setHagCount(String value) {
    hagCount = value;
  }

  @HopMetadataProperty private String hagMaxDistance = "";

  public String getHagMaxDistance() {
    return hagMaxDistance;
  }

  public void setHagMaxDistance(String value) {
    hagMaxDistance = value;
  }

  // ----- statistics -----

  @HopMetadataProperty private String statsDimensions = "";

  public String getStatsDimensions() {
    return statsDimensions;
  }

  public void setStatsDimensions(String value) {
    statsDimensions = value;
  }

  @HopMetadataProperty private String statsFields = "count,min,max,mean,stddev,variance";

  public String getStatsFields() {
    return statsFields;
  }

  public void setStatsFields(String value) {
    statsFields = value;
  }

  public List<String> selectedStatistics() {
    return tokens(statsFields).stream().map(field -> field.toLowerCase(Locale.ROOT)).toList();
  }

  // ----- raw -----

  @HopMetadataProperty private String rawMode = "APPEND";

  public String getRawMode() {
    return rawMode;
  }

  public void setRawMode(String value) {
    rawMode = value;
  }

  @HopMetadataProperty private String rawPipeline = "";

  public String getRawPipeline() {
    return rawPipeline;
  }

  public void setRawPipeline(String value) {
    rawPipeline = value;
  }

  // ----- point to rows -----

  @HopMetadataProperty private String rowDimensions = "";

  public String getRowDimensions() {
    return rowDimensions;
  }

  public void setRowDimensions(String value) {
    rowDimensions = value;
  }

  @HopMetadataProperty private String integerDimensions = "";

  public String getIntegerDimensions() {
    return integerDimensions;
  }

  public void setIntegerDimensions(String value) {
    integerDimensions = value;
  }

  @HopMetadataProperty private String maxPoints = "";

  public String getMaxPoints() {
    return maxPoints;
  }

  public void setMaxPoints(String value) {
    maxPoints = value;
  }

  // ----- merger -----

  @HopMetadataProperty private String groupField = "";

  public String getGroupField() {
    return groupField;
  }

  public void setGroupField(String value) {
    groupField = value;
  }

  @HopMetadataProperty private String batchSize = "";

  public String getBatchSize() {
    return batchSize;
  }

  public void setBatchSize(String value) {
    batchSize = value;
  }

  // ----- behaviour -----

  @Override
  public void setDefault() {
    // Defaults are initialized in the field declarations.
  }

  @Override
  public String getDialogClassName() {
    return PdalValueDialog.class.getName();
  }

  @Override
  public ITransform createTransform(
      TransformMeta transformMeta,
      ITransformData data,
      int copyNr,
      PipelineMeta pipelineMeta,
      org.apache.hop.pipeline.Pipeline pipeline) {
    return new PdalValueTransform(
        transformMeta, this, (PdalValueData) data, copyNr, pipelineMeta, pipeline);
  }

  @Override
  public ITransformData createTransformData() {
    return new PdalValueData();
  }

  @Override
  public boolean supportsErrorHandling() {
    return true;
  }

  public void validateSettings() {
    if (version != 1) {
      throw new IllegalArgumentException(
          "Legacy point cloud configuration (value version 1 expected): migrate the transform");
    }
    if (operation() != Operation.MERGER && (valueField == null || valueField.isBlank())) {
      throw new IllegalArgumentException("Point Cloud field is required");
    }
    String op = operation().name();
    if ((op.equals("READER") || op.equals("WRITER")) && (source == null || source.isBlank())) {
      throw new IllegalArgumentException(
          op.equals("READER") ? "Point cloud source is required" : "Output file is required");
    }
    if (op.equals("CROP")) {
      for (String bound : List.of(minX, minY, maxX, maxY)) {
        if (bound == null || bound.isBlank()) {
          throw new IllegalArgumentException("Crop bounds minX, minY, maxX and maxY are required");
        }
      }
    }
    if (op.equals("REPROJECT") && !targetCrsField && (targetCrs == null || targetCrs.isBlank())) {
      throw new IllegalArgumentException("Target CRS is required");
    }
    if (op.equals("FILTER") && (expression == null || expression.isBlank())) {
      throw new IllegalArgumentException("Filter expression is required");
    }
    if (op.equals("RANGE") && (limits == null || limits.isBlank())) {
      throw new IllegalArgumentException("Range limits are required");
    }
    if (op.equals("CALCULATOR") && (assignment == null || assignment.isBlank())) {
      throw new IllegalArgumentException("Calculator assignment is required");
    }
    if (op.equals("CLASSIFICATION") && (classification == null || classification.isBlank())) {
      throw new IllegalArgumentException("Classification value is required");
    }
    if (op.equals("TRANSFORM") && (matrix == null || matrix.isBlank())) {
      throw new IllegalArgumentException("Transformation matrix is required");
    }
    if (op.equals("GROUND")
        && "DEM".equals(hagType)
        && computeHag
        && (hagDemFile == null || hagDemFile.isBlank())) {
      throw new IllegalArgumentException("A DEM file is required for height above ground from DEM");
    }
    if (op.equals("RAW") && (rawPipeline == null || rawPipeline.isBlank())) {
      throw new IllegalArgumentException("Raw pipeline JSON is required");
    }
    if (op.equals("TO_ROWS") && tokens(rowDimensions).isEmpty()) {
      throw new IllegalArgumentException("Point dimensions are required");
    }
    if (op.equals("STATISTICS")) {
      if (tokens(statsDimensions).isEmpty()) {
        throw new IllegalArgumentException("Statistics dimensions are required");
      }
      for (String statistic : selectedStatistics()) {
        if (!PdalStatistics.SUPPORTED_STATISTICS.contains(statistic)) {
          throw new IllegalArgumentException("Unsupported statistic: " + statistic);
        }
      }
    }
  }

  /** Field name the transform writes the resulting value to. */
  public String resultField(IVariables vars) {
    if (operation() == Operation.READER || outputValueField == null || outputValueField.isBlank()) {
      return vars.resolve(valueField);
    }
    return vars.resolve(outputValueField);
  }

  public List<String> selectedInfoFields() {
    return tokens(infoFields).stream().map(field -> field.toLowerCase(Locale.ROOT)).toList();
  }

  public static List<String> tokens(String value) {
    if (value == null || value.isBlank()) return List.of();
    return List.of(value.split(",")).stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
  }

  @Override
  public void getFields(
      IRowMeta row,
      String origin,
      IRowMeta[] info,
      TransformMeta next,
      IVariables vars,
      IHopMetadataProvider provider) {
    validateSettings();
    String op = operation().name();

    if (operation() == Operation.MERGER) {
      // Aggregating transform: the output row contains the group field and the merged value.
      IValueMeta group = null;
      if (groupField != null && !groupField.isBlank()) {
        int index = row.indexOfValue(vars.resolve(groupField));
        if (index < 0) {
          throw new IllegalArgumentException("Group field is missing: " + groupField);
        }
        group = row.getValueMeta(index);
      }
      row.clear();
      if (group != null) {
        row.addValueMeta(group);
      }
      row.addValueMeta(new ValueMetaPointCloud(vars.resolve(resultField(vars))));
      return;
    }

    String input = vars.resolve(valueField);
    if (!op.equals("READER")) {
      int index = row.indexOfValue(input);
      if (index < 0 || row.getValueMeta(index).getType() != ValueMetaPointCloud.TYPE_POINT_CLOUD) {
        throw new IllegalArgumentException("Expected Point Cloud field: " + input);
      }
    }

    if (op.equals("READER")) {
      add(row, origin, new ValueMetaPointCloud(input));
    } else if (op.equals("CROP")
        || op.equals("REPROJECT")
        || op.equals("FILTER")
        || op.equals("CALCULATOR")
        || op.equals("CLASSIFICATION")
        || op.equals("THIN")
        || op.equals("OUTLIER")
        || op.equals("SORT")
        || op.equals("TRANSFORM")
        || op.equals("GROUND")
        || op.equals("RAW")) {
      String name = resultField(vars);
      if (!name.equals(input)) add(row, origin, new ValueMetaPointCloud(name));
    } else if (op.equals("WRITER")) {
      add(row, origin, new ValueMetaString(vars.resolve(prefix) + "output_file"));
      add(row, origin, new ValueMetaString(vars.resolve(prefix) + "status"));
    } else if (op.equals("TO_ROWS")) {
      List<String> integer = tokens(integerDimensions);
      for (String dimension : tokens(rowDimensions)) {
        String name = vars.resolve(prefix) + dimension;
        add(
            row,
            origin,
            integer.contains(dimension) ? new ValueMetaInteger(name) : new ValueMetaNumber(name));
      }
    } else if (op.equals("STATISTICS")) {
      add(row, origin, new ValueMetaInteger(vars.resolve(prefix) + "point_count"));
      for (String dimension : tokens(statsDimensions)) {
        for (String statistic : selectedStatistics()) {
          String name = vars.resolve(prefix) + statistic + "_" + dimension;
          add(
              row,
              origin,
              statistic.equals("count")
                  ? new ValueMetaInteger(name)
                  : new ValueMetaNumber(name));
        }
      }
    } else if (op.equals("INFO")) {
      for (String field : selectedInfoFields()) {
        String name = vars.resolve(prefix) + field;
        add(
            row,
            origin,
            switch (field) {
              case "point_count" -> new ValueMetaInteger(name);
              case "minx", "miny", "minz", "maxx", "maxy", "maxz" -> new ValueMetaNumber(name);
              case "bounds", "crs", "dimensions" -> new ValueMetaString(name);
              default -> throw new IllegalArgumentException("Unknown point cloud information: " + field);
            });
      }
    }
  }

  private static void add(IRowMeta row, String origin, IValueMeta value) {
    value.setOrigin(origin);
    row.addValueMeta(value);
  }
}
