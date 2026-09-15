package ch.so.agi.hop.pdal.values;

import ch.so.agi.hop.pointcloud.PdalPlan;
import ch.so.agi.hop.pointcloud.PdalStage;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Renders point cloud plans as PDAL pipeline JSON and infers reader/writer stage types from file
 * extensions.
 *
 * <p>Option values are plain text. Values that are valid JSON booleans, {@code null} or numbers are
 * embedded without quotes, everything else is rendered as a JSON string. This keeps paths, CRS
 * codes and bounds strings intact while numeric and boolean provider options stay typed.
 */
public final class PdalPipelineJson {
  private static final Pattern NUMBER = Pattern.compile("-?(0|[1-9]\\d*)(\\.\\d+)?([eE][+-]?\\d+)?");

  private static final List<Map.Entry<String, String>> READER_TYPES =
      List.of(
          Map.entry(".copc.laz", "readers.copc"),
          Map.entry(".laz", "readers.las"),
          Map.entry(".las", "readers.las"),
          Map.entry("ept.json", "readers.ept"),
          Map.entry(".ept", "readers.ept"),
          Map.entry(".bpf", "readers.bpf"),
          Map.entry(".ply", "readers.ply"),
          Map.entry(".pcd", "readers.pcd"),
          Map.entry(".e57", "readers.e57"),
          Map.entry(".txt", "readers.text"),
          Map.entry(".csv", "readers.text"),
          Map.entry(".xyz", "readers.text"),
          Map.entry(".tif", "readers.gdal"),
          Map.entry(".tiff", "readers.gdal"),
          Map.entry(".vrt", "readers.gdal"));

  private static final List<Map.Entry<String, String>> WRITER_TYPES =
      List.of(
          Map.entry(".copc.laz", "writers.copc"),
          Map.entry(".laz", "writers.las"),
          Map.entry(".las", "writers.las"),
          Map.entry(".bpf", "writers.bpf"),
          Map.entry(".ply", "writers.ply"),
          Map.entry(".txt", "writers.text"),
          Map.entry(".csv", "writers.text"),
          Map.entry(".xyz", "writers.text"),
          Map.entry(".tif", "writers.gdal"),
          Map.entry(".tiff", "writers.gdal"));

  private PdalPipelineJson() {}

  /** Infers the PDAL reader stage type from the file extension of a source path or URL. */
  public static String readerType(String location) {
    return stageType(location, READER_TYPES, "source");
  }

  /** Infers the PDAL writer stage type from the file extension of an output path. */
  public static String writerType(String location) {
    return stageType(location, WRITER_TYPES, "output");
  }

  private static String stageType(
      String location, List<Map.Entry<String, String>> types, String role) {
    if (location == null || location.isBlank()) {
      throw new IllegalArgumentException("Point cloud " + role + " is required");
    }
    String path = location;
    int query = path.indexOf('?');
    if (query >= 0) path = path.substring(0, query);
    String lower = path.toLowerCase(Locale.ROOT);
    for (Map.Entry<String, String> entry : types) {
      if (lower.endsWith(entry.getKey())) return entry.getValue();
    }
    throw new IllegalArgumentException(
        "Unsupported point cloud "
            + role
            + " format: "
            + location
            + " (supported: "
            + types.stream().map(Map.Entry::getKey).distinct().toList()
            + ")");
  }

  /** Renders a complete plan as a PDAL pipeline document. */
  public static String render(PdalPlan plan) {
    StringBuilder json = new StringBuilder("{\"pipeline\":[");
    for (int i = 0; i < plan.size(); i++) {
      if (i > 0) json.append(',');
      json.append(renderStage(plan.stages().get(i)));
    }
    return json.append("]}").toString();
  }

  /** Renders a single stage as a PDAL stage JSON object. */
  public static String renderStage(PdalStage stage) {
    StringBuilder json = new StringBuilder("{\"type\":\"").append(escape(stage.type())).append('"');
    for (Map.Entry<String, String> option : stage.options().entrySet()) {
      json.append(",\"")
          .append(escape(option.getKey()))
          .append("\":")
          .append(renderValue(option.getValue()));
    }
    return json.append('}').toString();
  }

  static String renderValue(String value) {
    if (value == null) return "null";
    if (value.equals("true") || value.equals("false") || value.equals("null")) return value;
    if (NUMBER.matcher(value).matches()) return value;
    return "\"" + escape(value) + "\"";
  }

  static String escape(String value) {
    StringBuilder escaped = new StringBuilder(value.length() + 8);
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '"' -> escaped.append("\\\"");
        case '\\' -> escaped.append("\\\\");
        case '\n' -> escaped.append("\\n");
        case '\r' -> escaped.append("\\r");
        case '\t' -> escaped.append("\\t");
        case '\b' -> escaped.append("\\b");
        case '\f' -> escaped.append("\\f");
        default -> {
          if (c < 0x20) escaped.append(String.format("\\u%04x", (int) c));
          else escaped.append(c);
        }
      }
    }
    return escaped.toString();
  }
}
