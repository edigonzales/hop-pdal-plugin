package ch.so.agi.hop.pdal.values;

import static org.assertj.core.api.Assertions.*;

import ch.so.agi.hop.pointcloud.PdalPlan;
import ch.so.agi.hop.pointcloud.PdalStage;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PdalPipelineJsonTest {

  @Test
  void infersReaderTypes() {
    assertThat(PdalPipelineJson.readerType("/data/tile.laz")).isEqualTo("readers.las");
    assertThat(PdalPipelineJson.readerType("/data/tile.LAS")).isEqualTo("readers.las");
    assertThat(PdalPipelineJson.readerType("/data/tile.copc.laz")).isEqualTo("readers.copc");
    assertThat(PdalPipelineJson.readerType("https://example.org/ept.json")).isEqualTo("readers.ept");
    assertThat(PdalPipelineJson.readerType("/data/points.xyz")).isEqualTo("readers.text");
    assertThat(PdalPipelineJson.readerType("https://example.org/tile.laz?token=1"))
        .isEqualTo("readers.las");
  }

  @Test
  void infersWriterTypes() {
    assertThat(PdalPipelineJson.writerType("/data/out.laz")).isEqualTo("writers.las");
    assertThat(PdalPipelineJson.writerType("/data/out.copc.laz")).isEqualTo("writers.copc");
    assertThat(PdalPipelineJson.writerType("/data/out.xyz")).isEqualTo("writers.text");
  }

  @Test
  void rejectsUnsupportedFormats() {
    assertThatThrownBy(() -> PdalPipelineJson.readerType("/data/tile.xyz2"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unsupported point cloud source format");
    assertThatThrownBy(() -> PdalPipelineJson.writerType(""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("required");
  }

  @Test
  void rendersPipelineWithEscaping() {
    var options = new LinkedHashMap<String, String>();
    options.put("filename", "/data/ti\"le.laz");
    options.put("count", "1000");
    options.put("extra_dims", "all");

    String json =
        PdalPipelineJson.render(
            PdalPlan.of(
                PdalStage.of("readers.las", options),
                PdalStage.of("filters.crop", Map.of("bounds", "([0,1],[0,1])")),
                PdalStage.of("writers.las", Map.of("filename", "/data/out.laz", "overwrite", "true"))));

    assertThat(json)
        .startsWith("{\"pipeline\":[")
        .endsWith("]}")
        .contains("\"type\":\"readers.las\"")
        .contains("\"filename\":\"/data/ti\\\"le.laz\"")
        .contains("\"count\":1000")
        .contains("\"bounds\":\"([0,1],[0,1])\"")
        .contains("\"overwrite\":true")
        .doesNotContain("\\u0000");
  }

  @Test
  void rendersValuesByJsonLiteralRule() {
    assertThat(PdalPipelineJson.renderValue("true")).isEqualTo("true");
    assertThat(PdalPipelineJson.renderValue("42")).isEqualTo("42");
    assertThat(PdalPipelineJson.renderValue("-1.5e3")).isEqualTo("-1.5e3");
    assertThat(PdalPipelineJson.renderValue("null")).isEqualTo("null");
    assertThat(PdalPipelineJson.renderValue("EPSG:2056")).isEqualTo("\"EPSG:2056\"");
    assertThat(PdalPipelineJson.renderValue("(0,1)")).isEqualTo("\"(0,1)\"");
    assertThat(PdalPipelineJson.escape("a\nb\tc")).isEqualTo("a\\nb\\tc");
  }
}
