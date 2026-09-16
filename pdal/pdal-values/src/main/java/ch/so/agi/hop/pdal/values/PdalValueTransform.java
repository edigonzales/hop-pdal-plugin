package ch.so.agi.hop.pdal.values;

import ch.so.agi.hop.pointcloud.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.exception.HopTransformException;
import org.apache.hop.core.row.RowDataUtil;
import org.apache.hop.core.row.RowMeta;
import org.apache.hop.pipeline.Pipeline;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.BaseTransform;
import org.apache.hop.pipeline.transform.TransformMeta;

/**
 * Shared transform of the point cloud operations. Planning operations append stages to the value
 * plan; the reader describes the source and the writer executes the accumulated plan once. The
 * merger aggregates several unprocessed point clouds into one plan with a merge stage.
 */
public final class PdalValueTransform extends BaseTransform<PdalValueMeta, PdalValueData> {
  private static final Pattern ASSIGNED_DIMENSION =
      Pattern.compile("^\\s*([A-Za-z][A-Za-z0-9_]*)\\s*(\\[[^]]*])?\\s*=");

  public PdalValueTransform(
      TransformMeta transformMeta,
      PdalValueMeta meta,
      PdalValueData data,
      int copyNr,
      PipelineMeta pipelineMeta,
      Pipeline pipeline) {
    super(transformMeta, meta, data, copyNr, pipelineMeta, pipeline);
  }

  @Override
  public boolean processRow() throws HopException {
    if (meta.operation() == PdalValueMeta.Operation.MERGER) {
      return processMergerRow();
    }

    boolean standalone =
        meta.operation() == PdalValueMeta.Operation.READER
            && getPipelineMeta().findPreviousTransforms(getTransformMeta()).isEmpty();
    if (isStopped()) {
      return false;
    }

    Object[] row = standalone ? (data.emitted ? null : new Object[0]) : getRow();
    if (row == null) {
      setOutputDone();
      return false;
    }
    data.emitted = true;

    if (data.outputMeta == null) {
      data.inputMeta = standalone ? new RowMeta() : getInputRowMeta();
      data.outputMeta = data.inputMeta.clone();
      try {
        meta.getFields(data.outputMeta, getTransformName(), null, null, this, null);
      } catch (Exception e) {
        throw new HopTransformException("Invalid point cloud settings", e);
      }
    }

    PointCloudDataset dataset = null;
    try {
      Object[] result = RowDataUtil.resizeArray(row, data.outputMeta.size());
      switch (meta.operation()) {
        case READER -> result[index(meta.getValueField())] = readDataset(row);
        case INFO -> {
          dataset = dataset(row);
          fillInfo(result, dataset);
        }
        case CROP -> {
          dataset = dataset(row);
          result[index(meta.resultField(this))] = crop(dataset, row);
        }
        case REPROJECT -> {
          dataset = dataset(row);
          result[index(meta.resultField(this))] = reproject(dataset, row);
        }
        case WRITER -> {
          dataset = dataset(row);
          writeDataset(result, dataset, row);
        }
        case FILTER -> {
          dataset = dataset(row);
          result[index(meta.resultField(this))] =
              dataset.append(
                  PdalOperationStages.expression(
                      text(meta.getExpression()), meta.isInvertFilter(), text(meta.getWhere())),
                  dataset.descriptor().withPointCount(PointCloudDescriptor.UNKNOWN_POINT_COUNT));
        }
        case RANGE -> {
          dataset = dataset(row);
          result[index(meta.resultField(this))] =
              dataset.append(
                  PdalOperationStages.range(text(meta.getLimits()), text(meta.getWhere())),
                  dataset.descriptor().withPointCount(PointCloudDescriptor.UNKNOWN_POINT_COUNT));
        }
        case CALCULATOR -> {
          dataset = dataset(row);
          String assignment = text(meta.getAssignment());
          var descriptor = dataset.descriptor();
          for (String name : assignedDimensions(assignment)) {
            descriptor = withDimension(descriptor, name);
          }
          result[index(meta.resultField(this))] =
              dataset.append(
                  PdalOperationStages.assign(
                      text(meta.getAssignmentKind()),
                      assignment,
                      text(meta.getCondition()),
                      text(meta.getWhere())),
                  descriptor);
        }
        case CLASSIFICATION -> {
          dataset = dataset(row);
          result[index(meta.resultField(this))] =
              dataset.append(
                  PdalOperationStages.classification(
                      text(meta.getClassification()),
                      meta.isOnlyUnclassified(),
                      text(meta.getWhere())),
                  dataset.descriptor());
        }
        case THIN -> {
          dataset = dataset(row);
          result[index(meta.resultField(this))] =
              dataset.append(
                  PdalOperationStages.thin(
                      text(meta.getThinType()),
                      text(meta.getStep()),
                      text(meta.getVoxelCell()),
                      text(meta.getVoxelMode()),
                      text(meta.getGridCell()),
                      text(meta.getSampleRadius()),
                      text(meta.getFpsCount()),
                      text(meta.getWhere())),
                  dataset.descriptor().withPointCount(PointCloudDescriptor.UNKNOWN_POINT_COUNT));
        }
        case OUTLIER -> {
          dataset = dataset(row);
          result[index(meta.resultField(this))] =
              dataset.append(
                  PdalOperationStages.outlier(
                      text(meta.getOutlierType()),
                      text(meta.getMeanK()),
                      text(meta.getMultiplier()),
                      text(meta.getRadius()),
                      text(meta.getMinK()),
                      text(meta.getWhere())),
                  dataset.descriptor().withPointCount(PointCloudDescriptor.UNKNOWN_POINT_COUNT));
        }
        case SORT -> {
          dataset = dataset(row);
          result[index(meta.resultField(this))] =
              dataset.append(
                  PdalOperationStages.sort(
                      meta.isMortonOrder(),
                      meta.isMortonReverse(),
                      text(meta.getSortDimensions()),
                      meta.isSortDescending(),
                      text(meta.getWhere())),
                  dataset.descriptor());
        }
        case TRANSFORM -> {
          dataset = dataset(row);
          result[index(meta.resultField(this))] =
              dataset.append(
                  PdalOperationStages.transformation(text(meta.getMatrix()), text(meta.getWhere())),
                  dataset.descriptor());
        }
        case GROUND -> {
          dataset = dataset(row);
          result[index(meta.resultField(this))] = ground(dataset);
        }
        case RAW -> {
          dataset = dataset(row);
          PdalPlan raw = PdalOperationStages.raw(text(meta.getRawPipeline()), text(meta.getRawMode()));
          PdalPlan plan =
              "REPLACE".equals(text(meta.getRawMode())) ? raw : dataset.plan().append(raw);
          result[index(meta.resultField(this))] =
              PointCloudDataset.of(
                  dataset.source(),
                  dataset.descriptor().withPointCount(PointCloudDescriptor.UNKNOWN_POINT_COUNT),
                  plan);
        }
        default -> throw new IllegalStateException("Unsupported operation: " + meta.operation());
      }
      putRow(data.outputMeta, result);
    } catch (Exception e) {
      if (isStopped()) {
        return false;
      }
      String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
      if (dataset != null) {
        message =
            "Point cloud source "
                + dataset.source().location()
                + "; consumer "
                + getTransformName()
                + ": "
                + message;
      }
      var error = getTransformMeta().getTransformErrorMeta();
      if (getTransformMeta().isDoingErrorHandling() || (error != null && error.isEnabled())) {
        putError(
            data.inputMeta,
            row,
            1L,
            message,
            meta.getValueField(),
            "POINTCLOUD_" + meta.operation() + "_ERROR");
      } else {
        throw new HopTransformException(message, e);
      }
    }
    return true;
  }

  // ----- merger -----

  private boolean processMergerRow() throws HopException {
    if (isStopped()) {
      return false;
    }
    Object[] row = getRow();
    if (row == null) {
      if (first) {
        first = false;
      } else {
        emitMergedGroups(true);
      }
      setOutputDone();
      return false;
    }

    if (first) {
      first = false;
      data.inputMeta = getInputRowMeta();
      data.outputMeta = mergerOutputMeta();
    }

    try {
      int valueIndex = data.inputMeta.indexOfValue(resolve(meta.getValueField()));
      if (valueIndex < 0 || !(row[valueIndex] instanceof PointCloudDataset)) {
        throw new IllegalArgumentException(
            "Missing or null Point Cloud value: " + meta.getValueField());
      }
      PointCloudDataset dataset = (PointCloudDataset) row[valueIndex];
      String key = groupKey(row);
      PdalValueData.MergedGroup group =
          data.mergedGroups.computeIfAbsent(key, k -> new PdalValueData.MergedGroup(groupValue(row)));
      group.datasets.add(dataset);

      int batchSize = batchSize();
      if (batchSize > 0 && group.datasets.size() >= batchSize) {
        emitGroup(key, group, true);
      }
    } catch (Exception e) {
      if (isStopped()) {
        return false;
      }
      String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
      var error = getTransformMeta().getTransformErrorMeta();
      if (getTransformMeta().isDoingErrorHandling() || (error != null && error.isEnabled())) {
        putError(
            data.inputMeta, row, 1L, message, meta.getValueField(), "POINTCLOUD_MERGER_ERROR");
      } else {
        throw new HopTransformException(message, e);
      }
    }
    return true;
  }

  private void emitMergedGroups(boolean onlyRemaining) throws HopTransformException {
    for (String key : List.copyOf(data.mergedGroups.keySet())) {
      emitGroup(key, data.mergedGroups.get(key), false);
    }
  }

  private void emitGroup(String key, PdalValueData.MergedGroup group, boolean keepGroup)
      throws HopTransformException {
    try {
      PointCloudDataset merged =
          PointCloudDataset.of(
              group.datasets.get(0).source(),
              PdalOperationStages.mergeDescriptor(group.datasets),
              PdalOperationStages.mergePlan(group.datasets));
      group.batchesEmitted++;
      Object[] out = RowDataUtil.allocateRowData(data.outputMeta.size());
      int index = 0;
      if (group.key != null || hasGroupField()) {
        out[index++] = group.key;
      }
      out[index] = merged;
      if (batchSize() > 0) {
        out[index + 1] = (long) group.batchesEmitted;
      }
      putRow(data.outputMeta, out);
      logBasic(
          "Merged "
              + group.datasets.size()
              + " point clouds with "
              + merged.descriptor().dimensions().size()
              + " dimensions (batch "
              + group.batchesEmitted
              + ")");
      if (keepGroup) {
        group.datasets.clear();
      } else {
        data.mergedGroups.remove(key);
      }
    } catch (HopException e) {
      throw e;
    } catch (Exception e) {
      throw new HopTransformException(e.getMessage(), e);
    }
  }

  private RowMeta mergerOutputMeta() {
    var outputMeta = new RowMeta();
    if (hasGroupField()) {
      int index = data.inputMeta.indexOfValue(resolve(meta.getGroupField()));
      if (index < 0) {
        throw new IllegalArgumentException("Group field is missing: " + meta.getGroupField());
      }
      outputMeta.addValueMeta(data.inputMeta.getValueMeta(index));
    }
    var value = new ch.so.agi.hop.pointcloud.type.ValueMetaPointCloud(resolve(meta.resultField(this)));
    value.setOrigin(getTransformName());
    outputMeta.addValueMeta(value);
    if (batchSize() > 0) {
      var batch = new org.apache.hop.core.row.value.ValueMetaInteger(resolve(meta.getPrefix()) + "batch");
      batch.setOrigin(getTransformName());
      outputMeta.addValueMeta(batch);
    }
    return outputMeta;
  }

  private boolean hasGroupField() {
    return meta.getGroupField() != null && !meta.getGroupField().isBlank();
  }

  private String groupKey(Object[] row) throws org.apache.hop.core.exception.HopValueException {
    if (!hasGroupField()) {
      return "";
    }
    int index = data.inputMeta.indexOfValue(resolve(meta.getGroupField()));
    if (index < 0) {
      throw new IllegalArgumentException("Group field is missing: " + meta.getGroupField());
    }
    String value = data.inputMeta.getString(row, index);
    return value == null ? "" : value;
  }

  private Object groupValue(Object[] row) {
    if (!hasGroupField()) {
      return null;
    }
    int index = data.inputMeta.indexOfValue(resolve(meta.getGroupField()));
    return row[index];
  }

  private int batchSize() {
    String value = text(meta.getBatchSize());
    if (value.isEmpty()) {
      return 0;
    }
    int size = Integer.parseInt(value);
    if (size < 0) {
      throw new IllegalArgumentException("Batch size must not be negative");
    }
    return size;
  }

  // ----- reader / writer -----

  private PointCloudDataset readDataset(Object[] row) throws Exception {
    String location = value(meta.getSource(), meta.isSourceField(), row);
    String readerType =
        text(meta.getDriver()).isEmpty()
            ? PdalPipelineJson.readerType(location)
            : "readers." + text(meta.getDriver());

    Map<String, String> options = new LinkedHashMap<>();
    options.put("filename", location);
    String overrideSrs = text(meta.getOverrideSrs());
    if (!overrideSrs.isEmpty()) {
      options.put("override_srs", overrideSrs);
    }
    String resolution = text(meta.getResolution());
    if (!resolution.isEmpty()) {
      if (!readerType.equals("readers.copc") && !readerType.equals("readers.ept")) {
        throw new IllegalArgumentException(
            "Resolution is only supported for COPC and EPT sources, but the reader is " + readerType);
      }
      options.put("resolution", resolution);
    }

    PointCloudDescriptor descriptor = data.backend.describe(location, readerType);
    return PointCloudDataset.of(
        new PointCloudReference(location, null),
        descriptor,
        PdalPlan.of(PdalStage.of(readerType, options)));
  }

  private void writeDataset(Object[] result, PointCloudDataset dataset, Object[] row)
      throws Exception {
    String output = value(meta.getSource(), meta.isSourceField(), row);
    String writerType = PdalPipelineJson.writerType(output);

    Path outputPath = Path.of(output).toAbsolutePath().normalize();
    if (Files.exists(outputPath)) {
      if (!meta.isOverwrite()) {
        throw new IllegalArgumentException(
            "Output file exists: " + outputPath + " (enable Overwrite)");
      }
      Files.delete(outputPath);
    }

    boolean lasWriter = writerType.equals("writers.las") || writerType.equals("writers.copc");
    Map<String, String> options = new LinkedHashMap<>();
    options.put("filename", outputPath.toString());

    String srs = text(meta.getOverrideSrs());
    if (srs.isEmpty()) {
      srs =
          dataset.descriptor().authority() != null
              ? dataset.descriptor().authority()
              : dataset.descriptor().crsWkt();
    }
    if (lasWriter && srs != null && !srs.isBlank()) {
      options.put("a_srs", srs);
    }
    putIfSet(options, "extra_dims", text(meta.getExtraDims()), lasWriter, writerType);
    putIfSet(
        options,
        "compression",
        text(meta.getCompression()),
        writerType.equals("writers.las"),
        writerType);
    putIfSet(
        options,
        "minor_version",
        text(meta.getMinorVersion()),
        writerType.equals("writers.las"),
        writerType);
    putIfSet(
        options,
        "threads",
        text(meta.getThreads()),
        writerType.equals("writers.copc"),
        writerType);
    putIfSet(options, "forward", text(meta.getForward()), lasWriter, writerType);

    PointCloudExecution execution =
        data.backend.execute(dataset.plan().append(PdalStage.of(writerType, options)), this::isStopped);
    result[index(meta.getPrefix() + "output_file")] = outputPath.toString();
    result[index(meta.getPrefix() + "status")] = "OK";
    logBasic("Wrote " + execution.pointCount() + " points to " + outputPath);
  }

  private static void putIfSet(
      Map<String, String> options, String option, String value, boolean applicable, String writerType) {
    if (value == null || value.isBlank()) {
      return;
    }
    if (!applicable) {
      throw new IllegalArgumentException(
          "Option '" + option + "' is not supported for " + writerType);
    }
    options.put(option, value);
  }

  // ----- info / crop / reproject / ground -----

  private void fillInfo(Object[] result, PointCloudDataset dataset) {
    PointCloudDescriptor descriptor = dataset.descriptor();
    for (String field : meta.selectedInfoFields()) {
      Object value =
          switch (field) {
            case "point_count" ->
                descriptor.hasPointCount() ? (Object) descriptor.pointCount() : null;
            case "crs" ->
                descriptor.authority() != null ? descriptor.authority() : descriptor.crsWkt();
            case "dimensions" ->
                descriptor.dimensions().stream()
                    .map(PointDimension::toString)
                    .collect(Collectors.joining(", "));
            case "bounds" ->
                descriptor.bounds().minX()
                    + ","
                    + descriptor.bounds().minY()
                    + ","
                    + descriptor.bounds().minZ()
                    + ","
                    + descriptor.bounds().maxX()
                    + ","
                    + descriptor.bounds().maxY()
                    + ","
                    + descriptor.bounds().maxZ();
            case "minx" -> descriptor.bounds().minX();
            case "miny" -> descriptor.bounds().minY();
            case "minz" -> descriptor.bounds().minZ();
            case "maxx" -> descriptor.bounds().maxX();
            case "maxy" -> descriptor.bounds().maxY();
            case "maxz" -> descriptor.bounds().maxZ();
            default -> throw new IllegalArgumentException("Unknown point cloud information: " + field);
          };
      result[index(meta.getPrefix() + field)] = value;
    }
  }

  private PointCloudDataset crop(PointCloudDataset dataset, Object[] row) throws Exception {
    double minX = number(meta.getMinX(), meta.isBoundsFields(), row);
    double minY = number(meta.getMinY(), meta.isBoundsFields(), row);
    double maxX = number(meta.getMaxX(), meta.isBoundsFields(), row);
    double maxY = number(meta.getMaxY(), meta.isBoundsFields(), row);
    if (minX >= maxX || minY >= maxY) {
      throw new IllegalArgumentException("Crop minimum must be below maximum");
    }

    boolean threeDimensional = !isBlank(meta.getMinZ()) || !isBlank(meta.getMaxZ());
    double minZ = 0;
    double maxZ = 0;
    String bounds;
    if (threeDimensional) {
      minZ = number(meta.getMinZ(), meta.isBoundsFields(), row);
      maxZ = number(meta.getMaxZ(), meta.isBoundsFields(), row);
      if (minZ > maxZ) {
        throw new IllegalArgumentException("Crop minimum must not be above maximum");
      }
      bounds =
          "(["
              + minX
              + ","
              + maxX
              + "],["
              + minY
              + ","
              + maxY
              + "],["
              + minZ
              + ","
              + maxZ
              + "])";
    } else {
      bounds = "([" + minX + "," + maxX + "],[" + minY + "," + maxY + "])";
    }

    PointCloudBounds sourceBounds = dataset.descriptor().bounds();
    double readerMinZ = threeDimensional ? minZ : sourceBounds.minZ();
    double readerMaxZ = threeDimensional ? maxZ : sourceBounds.maxZ();
    PointCloudDataset rewired =
        PdalOperationStages.remoteReaderClip(
            dataset, minX, minY, readerMinZ, maxX, maxY, readerMaxZ);
    if (rewired != dataset) {
      logBasic("Pushing crop bounds into the remote " + rewired.plan().stages().get(0).type() + " reader");
    }

    PointCloudBounds cropped =
        intersect(dataset.descriptor().bounds(), minX, minY, readerMinZ, maxX, maxY, readerMaxZ);
    return rewired.append(
        PdalStage.of("filters.crop", Map.of("bounds", bounds)),
        rewired
            .descriptor()
            .withBounds(cropped)
            .withPointCount(PointCloudDescriptor.UNKNOWN_POINT_COUNT));
  }

  private PointCloudDataset reproject(PointCloudDataset dataset, Object[] row) throws Exception {
    String crs = value(meta.getTargetCrs(), meta.isTargetCrsField(), row);
    boolean authorityCode = crs.matches("[A-Za-z]{2,10}:[0-9]+");
    PointCloudDescriptor descriptor =
        authorityCode
            ? dataset.descriptor().withCrs(null, crs)
            : dataset.descriptor().withCrs(crs, null);
    return dataset.append(PdalStage.of("filters.reprojection", Map.of("out_srs", crs)), descriptor);
  }

  private PointCloudDataset ground(PointCloudDataset dataset) throws Exception {
    var options =
        new PdalOperationStages.GroundOptions(
            text(meta.getGroundType()),
            text(meta.getScalar()),
            text(meta.getSlope()),
            text(meta.getThreshold()),
            text(meta.getWindow()),
            text(meta.getGroundCell()),
            text(meta.getPmfCellSize()),
            text(meta.getInitialDistance()),
            text(meta.getMaxWindowSize()),
            text(meta.getCsfResolution()),
            text(meta.getCsfRigidness()),
            text(meta.getWhere()));
    PdalPlan plan = PdalOperationStages.ground(options);
    PointCloudDescriptor descriptor = dataset.descriptor();
    if (meta.isComputeHag()) {
      plan =
          plan.append(
              PdalOperationStages.hag(
                  text(meta.getHagType()),
                  text(meta.getHagDemFile()),
                  text(meta.getHagCount()),
                  text(meta.getHagMaxDistance()),
                  text(meta.getWhere())));
      descriptor = withDimension(descriptor, "HeightAboveGround");
    }
    return dataset.append(plan, descriptor);
  }

  // ----- helpers -----

  private static PointCloudDescriptor withDimension(
      PointCloudDescriptor descriptor, String name) {
    for (PointDimension dimension : descriptor.dimensions()) {
      if (dimension.name().equals(name)) {
        return descriptor;
      }
    }
    var dimensions = new java.util.ArrayList<>(descriptor.dimensions());
    dimensions.add(PointDimension.of(name, PointDataType.FLOAT64));
    return descriptor.withDimensions(dimensions);
  }

  static List<String> assignedDimensions(String assignment) {
    if (assignment == null || assignment.isBlank()) {
      return List.of();
    }
    var names = new java.util.ArrayList<String>();
    for (String part : assignment.split(",")) {
      Matcher matcher = ASSIGNED_DIMENSION.matcher(part);
      if (matcher.find()) {
        names.add(matcher.group(1));
      }
    }
    return names;
  }

  private static PointCloudBounds intersect(
      PointCloudBounds source,
      double minX,
      double minY,
      double minZ,
      double maxX,
      double maxY,
      double maxZ) {
    double x0 = Math.max(source.minX(), minX);
    double x1 = Math.min(source.maxX(), maxX);
    double y0 = Math.max(source.minY(), minY);
    double y1 = Math.min(source.maxY(), maxY);
    double z0 = Math.max(source.minZ(), minZ);
    double z1 = Math.min(source.maxZ(), maxZ);
    if (x0 > x1) {
      double center = (x0 + x1) / 2;
      x0 = center;
      x1 = center;
    }
    if (y0 > y1) {
      double center = (y0 + y1) / 2;
      y0 = center;
      y1 = center;
    }
    if (z0 > z1) {
      double center = (z0 + z1) / 2;
      z0 = center;
      z1 = center;
    }
    return new PointCloudBounds(x0, y0, z0, x1, y1, z1);
  }

  private PointCloudDataset dataset(Object[] row) {
    int index = data.inputMeta.indexOfValue(resolve(meta.getValueField()));
    if (index < 0 || !(row[index] instanceof PointCloudDataset)) {
      throw new IllegalArgumentException(
          "Missing or null Point Cloud value: " + meta.getValueField());
    }
    return (PointCloudDataset) row[index];
  }

  private String value(String text, boolean field, Object[] row) throws Exception {
    String resolved = resolve(text == null ? "" : text).trim();
    if (!field) {
      if (resolved.isEmpty()) {
        throw new IllegalArgumentException("Value is required");
      }
      return resolved;
    }
    int index = data.inputMeta.indexOfValue(resolved);
    if (index < 0) {
      throw new IllegalArgumentException("Input field missing: " + resolved);
    }
    String value = data.inputMeta.getString(row, index);
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Input field empty: " + resolved);
    }
    return value.trim();
  }

  private double number(String text, boolean field, Object[] row) throws Exception {
    double value = Double.parseDouble(value(text, field, row));
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException("Expected a finite number");
    }
    return value;
  }

  private int index(String name) {
    int index = data.outputMeta.indexOfValue(resolve(name));
    if (index < 0) {
      throw new IllegalArgumentException("Output field missing: " + name);
    }
    return index;
  }

  private String text(String value) {
    return value == null ? "" : resolve(value).trim();
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
