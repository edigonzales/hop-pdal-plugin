package ch.so.agi.hop.pdal.values;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses the {@code filters.stats} section of PDAL pipeline metadata.
 *
 * <p>PDAL writes one entry per dimension with {@code count}, {@code minimum}, {@code maximum},
 * {@code average}, {@code stddev} and {@code variance} (plus {@code skewness} and
 * {@code kurtosis} with the advanced statistics option). The Hop field names use the short forms
 * {@code min}, {@code max} and {@code mean}.
 */
final class PdalStatistics {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final Map<String, String> METADATA_KEYS =
      Map.ofEntries(
          Map.entry("count", "count"),
          Map.entry("min", "minimum"),
          Map.entry("max", "maximum"),
          Map.entry("mean", "average"),
          Map.entry("stddev", "stddev"),
          Map.entry("variance", "variance"),
          Map.entry("skewness", "skewness"),
          Map.entry("kurtosis", "kurtosis"));

  static final List<String> SUPPORTED_STATISTICS =
      List.of("count", "min", "max", "mean", "stddev", "variance", "skewness", "kurtosis");

  private PdalStatistics() {}

  static String metadataKey(String statistic) {
    String key = METADATA_KEYS.get(statistic.toLowerCase(java.util.Locale.ROOT));
    if (key == null) {
      throw new IllegalArgumentException("Unsupported statistic: " + statistic);
    }
    return key;
  }

  static boolean requiresAdvanced(List<String> statistics) {
    return statistics.stream()
        .map(statistic -> statistic.toLowerCase(java.util.Locale.ROOT))
        .anyMatch(statistic -> statistic.equals("skewness") || statistic.equals("kurtosis"));
  }

  /** Returns {@code dimension -> statistic -> value} from the metadata JSON. */
  static Map<String, Map<String, Double>> parse(String metadataJson) {
    Map<String, Map<String, Double>> statistics = new LinkedHashMap<>();
    if (metadataJson == null || metadataJson.isBlank()) {
      return statistics;
    }
    try {
      JsonNode root = MAPPER.readTree(metadataJson);
      JsonNode entries =
          root.path("stages").path("filters.stats").path("statistic");
      if (!entries.isArray()) {
        return statistics;
      }
      for (JsonNode entry : entries) {
        String name = entry.path("name").asText("");
        if (name.isEmpty()) {
          continue;
        }
        Map<String, Double> values = new LinkedHashMap<>();
        entry
            .fields()
            .forEachRemaining(
                field -> {
                  if (field.getValue().isNumber()) {
                    values.put(field.getKey(), field.getValue().asDouble());
                  }
                });
        statistics.put(name, values);
      }
      return statistics;
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalArgumentException("Pipeline metadata is not valid JSON: " + e.getOriginalMessage());
    }
  }
}
