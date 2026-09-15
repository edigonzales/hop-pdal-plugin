package ch.so.agi.hop.pdal.values;

import ch.so.agi.hop.pointcloud.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.exception.HopTransformException;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.RowDataUtil;
import org.apache.hop.core.row.RowMeta;
import org.apache.hop.pipeline.Pipeline;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.BaseTransform;
import org.apache.hop.pipeline.transform.TransformMeta;

/**
 * Shared transform of the point cloud operations. The reader creates a value, info reads it,
 * crop/reproject plan an additional stage and the writer executes the accumulated plan once.
 */
public final class PdalValueTransform extends BaseTransform<PdalValueMeta, PdalValueData> {
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

  private PointCloudDataset readDataset(Object[] row) throws Exception {
    String location = value(meta.getSource(), meta.isSourceField(), row);
    PointCloudDescriptor descriptor = data.backend.describe(location);
    String readerType = PdalPipelineJson.readerType(location);
    return PointCloudDataset.of(
        new PointCloudReference(location, null),
        descriptor,
        PdalPlan.of(PdalStage.of(readerType, Map.of("filename", location))));
  }

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

    boolean threeDimensional =
        !isBlank(meta.getMinZ()) || !isBlank(meta.getMaxZ());
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

    PointCloudBounds cropped =
        intersect(dataset.descriptor().bounds(), minX, minY, minZ, maxX, maxY, maxZ);
    return dataset.append(
        PdalStage.of("filters.crop", Map.of("bounds", bounds)),
        dataset
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

    Map<String, String> options = new LinkedHashMap<>();
    options.put("filename", outputPath.toString());

    PointCloudExecution execution =
        data.backend.execute(dataset.plan().append(PdalStage.of(writerType, options)), this::isStopped);
    result[index(meta.getPrefix() + "output_file")] = outputPath.toString();
    result[index(meta.getPrefix() + "status")] = "OK";
    logBasic("Wrote " + execution.pointCount() + " points to " + outputPath);
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
    int i = data.inputMeta.indexOfValue(resolve(meta.getValueField()));
    if (i < 0 || !(row[i] instanceof PointCloudDataset)) {
      throw new IllegalArgumentException(
          "Missing or null Point Cloud value: " + meta.getValueField());
    }
    return (PointCloudDataset) row[i];
  }

  private String value(String text, boolean field, Object[] row) throws Exception {
    String resolved = resolve(text == null ? "" : text).trim();
    if (!field) {
      if (resolved.isEmpty()) {
        throw new IllegalArgumentException("Value is required");
      }
      return resolved;
    }
    int i = data.inputMeta.indexOfValue(resolved);
    if (i < 0) {
      throw new IllegalArgumentException("Input field missing: " + resolved);
    }
    String value = data.inputMeta.getString(row, i);
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

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
