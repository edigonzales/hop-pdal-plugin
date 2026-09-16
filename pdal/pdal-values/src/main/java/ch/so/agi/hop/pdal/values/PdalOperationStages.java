package ch.so.agi.hop.pdal.values;

import ch.so.agi.hop.pointcloud.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

/**
 * Builds PDAL stages for the transform operations. Kept free of Hop runtime types so the stage
 * construction is unit testable.
 */
public final class PdalOperationStages {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private PdalOperationStages() {}

  /** Ground classification and height-above-ground options. */
  public record GroundOptions(
      String type,
      String scalar,
      String slope,
      String threshold,
      String window,
      String cell,
      String cellSize,
      String initialDistance,
      String maxWindowSize,
      String resolution,
      String rigidness,
      String where) {}

  static PdalStage stage(String type, Map<String, String> options) {
    return new PdalStage(type, options);
  }

  static Map<String, String> options(String... keyValues) {
    var options = new LinkedHashMap<String, String>();
    for (int i = 0; i + 1 < keyValues.length; i += 2) {
      String key = keyValues[i];
      String value = keyValues[i + 1];
      if (value != null && !value.isBlank()) {
        options.put(key, value);
      }
    }
    return options;
  }

  static String combineWhere(String first, String second) {
    boolean hasFirst = first != null && !first.isBlank();
    boolean hasSecond = second != null && !second.isBlank();
    if (hasFirst && hasSecond) return "(" + first + ") && (" + second + ")";
    if (hasFirst) return first;
    if (hasSecond) return second;
    return "";
  }

  public static PdalStage expression(String expression, boolean invert, String where) {
    String value = invert ? "!(" + expression + ")" : expression;
    return stage("filters.expression", options("expression", value, "where", where));
  }

  public static PdalStage range(String limits, String where) {
    return stage("filters.range", options("limits", limits, "where", where));
  }

  public static PdalStage assign(String kind, String assignment, String condition, String where) {
    var options = options("condition", condition, "where", where);
    if ("ASSIGNMENT".equals(kind)) {
      options.put("assignment", assignment);
    } else {
      options.put("value", assignment);
    }
    return stage("filters.assign", options);
  }

  public static PdalStage classification(
      String classification, boolean onlyUnclassified, String where) {
    String condition = onlyUnclassified ? "Classification == 0" : "";
    return stage(
        "filters.assign",
        options(
            "value",
            "Classification = " + classification,
            "where",
            combineWhere(condition, where)));
  }

  public static PdalStage thin(
      String type,
      String step,
      String voxelCell,
      String voxelMode,
      String gridCell,
      String sampleRadius,
      String fpsCount,
      String where) {
    return switch (type == null ? "" : type) {
      case "VOXEL_DOWNSIZE" ->
          stage(
              "filters.voxeldownsize",
              options("cell", voxelCell, "mode", voxelMode, "where", where));
      case "GRID_DECIMATION" ->
          stage("filters.gridDecimation", options("resolution", gridCell, "where", where));
      case "SAMPLE" -> stage("filters.sample", options("radius", sampleRadius, "where", where));
      case "FPS" -> stage("filters.fps", options("count", fpsCount, "where", where));
      default -> stage("filters.decimation", options("step", step, "where", where));
    };
  }

  public static PdalStage outlier(
      String type, String meanK, String multiplier, String radius, String minK, String where) {
    if ("RADIUS".equals(type)) {
      return stage(
          "filters.outlier",
          options("method", "radius", "radius", radius, "min_k", minK, "where", where));
    }
    return stage(
        "filters.outlier",
        options(
            "method",
            "statistical",
            "mean_k",
            meanK,
            "multiplier",
            multiplier,
            "where",
            where));
  }

  public static PdalStage sort(
      boolean morton, boolean reverse, String dimensions, boolean descending, String where) {
    if (morton) {
      return stage("filters.mortonorder", options("reverse", reverse ? "true" : "", "where", where));
    }
    return stage(
        "filters.sort",
        options("dimensions", dimensions, "order", descending ? "DESC" : "ASC", "where", where));
  }

  public static PdalStage transformation(String matrix, String where) {
    return stage("filters.transformation", options("matrix", matrix, "where", where));
  }

  public static PdalPlan ground(GroundOptions options) {
    var filterOptions =
        switch (options.type() == null ? "" : options.type()) {
          case "PMF" ->
              options(
                  "scalar",
                  options.scalar(),
                  "slope",
                  options.slope(),
                  "initial_distance",
                  options.initialDistance(),
                  "cell_size",
                  options.cellSize(),
                  "max_window_size",
                  options.maxWindowSize(),
                  "where",
                  options.where());
          case "CSF" ->
              options(
                  "resolution",
                  options.resolution(),
                  "rigidness",
                  options.rigidness(),
                  "threshold",
                  options.threshold(),
                  "where",
                  options.where());
          default ->
              options(
                  "scalar",
                  options.scalar(),
                  "slope",
                  options.slope(),
                  "threshold",
                  options.threshold(),
                  "window",
                  options.window(),
                  "cell",
                  options.cell(),
                  "where",
                  options.where());
        };
    String filterType =
        switch (options.type() == null ? "" : options.type()) {
          case "PMF" -> "filters.pmf";
          case "CSF" -> "filters.csf";
          default -> "filters.smrf";
        };
    var stages = new ArrayList<PdalStage>();
    stages.add(stage(filterType, filterOptions));
    return new PdalPlan(stages);
  }

  public static PdalStage hag(
      String type, String demFile, String count, String maxDistance, String where) {
    return switch (type == null ? "" : type) {
      case "DELAUNAY" ->
          stage("filters.hag_delaunay", options("count", count, "where", where));
      case "DEM" -> stage("filters.hag_dem", options("raster", demFile, "where", where));
      default ->
          stage(
              "filters.hag_nn",
              options("count", count, "max_distance", maxDistance, "where", where));
    };
  }

  public static PdalPlan raw(String json, String mode) {
    List<PdalStage> stages = parseRawStages(json);
    if ("REPLACE".equals(mode)) {
      if (stages.isEmpty()) {
        throw new IllegalArgumentException("Raw pipeline must contain at least a reader stage");
      }
      if (!stages.get(0).type().startsWith("readers.")) {
        throw new IllegalArgumentException(
            "Raw pipeline in REPLACE mode must start with a reader stage, found: "
                + stages.get(0).type());
      }
      return new PdalPlan(stages);
    }
    return new PdalPlan(stages);
  }

  static List<PdalStage> parseRawStages(String json) {
    if (json == null || json.isBlank()) {
      throw new IllegalArgumentException("Raw pipeline JSON is required");
    }
    try {
      JsonNode root = MAPPER.readTree(json);
      JsonNode pipeline = root.isArray() ? root : root.path("pipeline");
      if (!pipeline.isArray()) {
        throw new IllegalArgumentException(
            "Raw pipeline must be a JSON array of stages or an object with a 'pipeline' array");
      }
      var stages = new ArrayList<PdalStage>();
      for (JsonNode node : pipeline) {
        JsonNode type = node.path("type");
        if (!type.isTextual() || type.asText().isBlank()) {
          throw new IllegalArgumentException("Raw pipeline stage without 'type'");
        }
        var options = new LinkedHashMap<String, String>();
        node.fields()
            .forEachRemaining(
                entry -> {
                  if (entry.getKey().equals("type")) {
                    return;
                  }
                  JsonNode value = entry.getValue();
                  if (value.isContainerNode()) {
                    throw new IllegalArgumentException(
                        "Raw pipeline option '"
                            + entry.getKey()
                            + "' must be a scalar (strings, numbers, booleans)");
                  }
                  options.put(entry.getKey(), value.asText());
                });
        stages.add(new PdalStage(type.asText(), options));
      }
      return stages;
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalArgumentException("Raw pipeline is not valid JSON: " + e.getOriginalMessage());
    }
  }

  /**
   * Pushes crop bounds into a remote COPC/EPT reader so that only the intersecting octree nodes
   * are requested. The returned dataset is the same instance when no pushdown applies; the exact
   * clip still needs a following crop stage.
   */
  public static PointCloudDataset remoteReaderClip(
      PointCloudDataset dataset,
      double minX,
      double minY,
      double minZ,
      double maxX,
      double maxY,
      double maxZ) {
    if (dataset.plan().size() != 1 || !dataset.source().remote()) {
      return dataset;
    }
    PdalStage reader = dataset.plan().stages().get(0);
    if (!reader.type().equals("readers.copc") && !reader.type().equals("readers.ept")) {
      return dataset;
    }
    var options = new LinkedHashMap<>(reader.options());
    options.put(
        "bounds",
        "([" + minX + "," + maxX + "],[" + minY + "," + maxY + "],[" + minZ + "," + maxZ + "])");
    return PointCloudDataset.of(
        dataset.source(),
        dataset.descriptor(),
        PdalPlan.of(new PdalStage(reader.type(), options)));
  }

  public static PdalPlan mergePlan(List<PointCloudDataset> datasets) {
    var stages = new ArrayList<PdalStage>();
    for (PointCloudDataset dataset : datasets) {
      if (dataset.plan().size() != 1 || !dataset.plan().stages().get(0).type().startsWith("readers.")) {
        throw new IllegalArgumentException(
            "Only unprocessed point clouds can be merged; '"
                + dataset.source().location()
                + "' already has planned filters. Add filters after the merger.");
      }
      stages.addAll(dataset.plan().stages());
    }
    stages.add(PdalStage.of("filters.merge"));
    return new PdalPlan(stages);
  }

  public static PointCloudDescriptor mergeDescriptor(List<PointCloudDataset> datasets) {
    if (datasets.isEmpty()) {
      throw new IllegalArgumentException("At least one point cloud is required for a merge");
    }
    String crsWkt = null;
    String authority = null;
    long pointCount = 0;
    boolean countKnown = true;
    double minX = Double.POSITIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;
    double minZ = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY;
    double maxZ = Double.NEGATIVE_INFINITY;
    var dimensions = new LinkedHashMap<String, PointDimension>();

    for (PointCloudDataset dataset : datasets) {
      PointCloudDescriptor descriptor = dataset.descriptor();
      crsWkt = mergeReference(crsWkt, descriptor.crsWkt(), dataset.source().location(), "CRS");
      authority =
          mergeReference(authority, descriptor.authority(), dataset.source().location(), "CRS");
      if (descriptor.hasPointCount()) {
        pointCount += descriptor.pointCount();
      } else {
        countKnown = false;
      }
      PointCloudBounds bounds = descriptor.bounds();
      minX = Math.min(minX, bounds.minX());
      minY = Math.min(minY, bounds.minY());
      minZ = Math.min(minZ, bounds.minZ());
      maxX = Math.max(maxX, bounds.maxX());
      maxY = Math.max(maxY, bounds.maxY());
      maxZ = Math.max(maxZ, bounds.maxZ());
      for (PointDimension dimension : descriptor.dimensions()) {
        PointDimension existing = dimensions.get(dimension.name());
        if (existing == null) {
          dimensions.put(dimension.name(), dimension);
        } else if (existing.type() != dimension.type()
            && existing.type() != PointDataType.UNKNOWN
            && dimension.type() != PointDataType.UNKNOWN) {
          throw new IllegalArgumentException(
              "Dimension '"
                  + dimension.name()
                  + "' has different types ("
                  + existing.type()
                  + " / "
                  + dimension.type()
                  + ") in "
                  + dataset.source().location());
        }
      }
    }

    return new PointCloudDescriptor(
        crsWkt,
        authority,
        countKnown ? pointCount : PointCloudDescriptor.UNKNOWN_POINT_COUNT,
        new PointCloudBounds(minX, minY, minZ, maxX, maxY, maxZ),
        List.copyOf(dimensions.values()));
  }

  private static String mergeReference(
      String existing, String candidate, String location, String what) {
    if (candidate == null || candidate.isBlank()) {
      return existing;
    }
    if (existing != null && !existing.isBlank() && !existing.equals(candidate)) {
      throw new IllegalArgumentException(
          what + " mismatch while merging: '" + existing + "' and '" + candidate + "' from " + location);
    }
    return candidate;
  }
}
