package ch.so.agi.hop.pdal.values;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PdalStatisticsTest {

  private static final String METADATA =
      """
      {
        "stages": {
          "filters.stats": {
            "statistic": [
              { "name": "X", "count": 1000, "minimum": 0, "maximum": 100,
                "average": 50, "stddev": 28.9, "variance": 835.8, "skewness": 0.0, "kurtosis": -1.2 },
              { "name": "Z", "count": 1000, "minimum": 0, "maximum": 10,
                "average": 5, "stddev": 2.9, "variance": 8.3 }
            ]
          },
          "writers.las": { "filename": [ "out.laz" ] }
        }
      }
      """;

  @Test
  void parsesStatistics() {
    Map<String, Map<String, Double>> statistics = PdalStatistics.parse(METADATA);

    assertThat(statistics).containsKeys("X", "Z");
    assertThat(statistics.get("X"))
        .containsEntry("count", 1000.0)
        .containsEntry("minimum", 0.0)
        .containsEntry("maximum", 100.0)
        .containsEntry("average", 50.0);
    assertThat(statistics.get("Z")).doesNotContainKey("skewness");
  }

  @Test
  void mapsStatisticsToMetadataKeys() {
    assertThat(PdalStatistics.metadataKey("count")).isEqualTo("count");
    assertThat(PdalStatistics.metadataKey("min")).isEqualTo("minimum");
    assertThat(PdalStatistics.metadataKey("max")).isEqualTo("maximum");
    assertThat(PdalStatistics.metadataKey("mean")).isEqualTo("average");
    assertThat(PdalStatistics.metadataKey("stddev")).isEqualTo("stddev");
    assertThatThrownBy(() -> PdalStatistics.metadataKey("median"))
        .hasMessageContaining("Unsupported statistic");
  }

  @Test
  void detectsAdvancedStatistics() {
    assertThat(PdalStatistics.requiresAdvanced(List.of("count", "mean"))).isFalse();
    assertThat(PdalStatistics.requiresAdvanced(List.of("kurtosis"))).isTrue();
    assertThat(PdalStatistics.requiresAdvanced(List.of("SKEWNESS"))).isTrue();
  }

  @Test
  void toleratesMissingOrInvalidMetadata() {
    assertThat(PdalStatistics.parse("")).isEmpty();
    assertThat(PdalStatistics.parse("{\"stages\":{}}")).isEmpty();
    assertThatThrownBy(() -> PdalStatistics.parse("{oops"))
        .hasMessageContaining("not valid JSON");
  }
}
