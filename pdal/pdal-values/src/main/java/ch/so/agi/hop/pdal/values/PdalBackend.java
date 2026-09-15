package ch.so.agi.hop.pdal.values;

import ch.so.agi.hop.pointcloud.*;
import ch.so.agi.pdal.ffm.Pdal;
import ch.so.agi.pdal.ffm.PdalPreview;
import ch.so.agi.pdal.ffm.PdalResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * PDAL execution context for the Hop point cloud transforms.
 *
 * <p>Uses the bundled PDAL runtime of {@code pdal-ffm-core} / {@code pdal-ffm-natives}; no system
 * PDAL installation is required. One instance belongs to one transform copy and is not shared
 * between threads or pipeline branches.
 */
public final class PdalBackend implements PointCloudBackend {

  @Override
  public PointCloudDescriptor describe(String location) throws Exception {
    String readerType = PdalPipelineJson.readerType(location);
    String pipeline =
        PdalPipelineJson.render(
            PdalPlan.of(PdalStage.of(readerType, Map.of("filename", location))));

    PdalPreview preview = Pdal.preview(pipeline);

    List<PointDimension> dimensions = new ArrayList<>(preview.dimensions().size());
    for (PdalPreview.PdalDimension dimension : preview.dimensions()) {
      dimensions.add(new PointDimension(dimension.name(), dataType(dimension.type())));
    }
    if (dimensions.isEmpty()) {
      throw new IllegalStateException("No point dimensions reported for " + location);
    }

    return new PointCloudDescriptor(
        blankToNull(preview.srsWkt()),
        blankToNull(preview.srsAuthority()),
        preview.pointCount(),
        toBounds(preview.bounds()),
        dimensions);
  }

  @Override
  public PointCloudExecution execute(PdalPlan plan, BooleanSupplier stopped) throws Exception {
    if (stopped != null && stopped.getAsBoolean()) {
      throw new IllegalStateException("Cancelled before execution");
    }
    PdalResult result = Pdal.execute(PdalPipelineJson.render(plan));
    return new PointCloudExecution(result.pointCount(), result.metadataJson(), result.log());
  }

  private static PointDataType dataType(String type) {
    try {
      return PointDataType.valueOf(type);
    } catch (IllegalArgumentException e) {
      return PointDataType.UNKNOWN;
    }
  }

  private static PointCloudBounds toBounds(PdalPreview.Bounds bounds) {
    if (bounds == null) {
      return new PointCloudBounds(0, 0, 0, 0, 0, 0);
    }
    return new PointCloudBounds(
        bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ());
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }
}
