package ch.so.agi.hop.pdal.values;

import static org.assertj.core.api.Assertions.*;

import ch.so.agi.hop.pointcloud.*;
import ch.so.agi.pdal.ffm.Pdal;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

/**
 * Native tests for the block-wise point reader. Enable with
 * {@code PDAL_RUN_INTEGRATION=true}.
 */
@EnabledIfEnvironmentVariable(named = "PDAL_RUN_INTEGRATION", matches = "true")
class PdalBlockReaderNativeTest {

  @Test
  void readsPointsInBlocks(@TempDir Path tempDir) throws Exception {
    Path input = tempDir.resolve("blocks-input.laz");
    Pdal.execute(
        """
        {"pipeline":[
          {"type":"readers.faux","mode":"ramp","bounds":"([0,100],[0,100],[0,10])","count":1000},
          {"type":"writers.las","filename":"%s"}]}
        """
            .formatted(jsonPath(input)));

    PdalPlan plan =
        PdalPlan.of(PdalStage.of("readers.las", Map.of("filename", input.toString())));

    int points = 0;
    double lastX = -1;
    try (PdalBlockReader reader =
        PdalBlockReader.open(plan, List.of("X", "Y", "Z", "Classification"), 128)) {
      assertThat(reader.pointCount()).isEqualTo(1000);
      assertThat(reader.type(0)).isEqualTo(PointDataType.FLOAT64);
      assertThat(reader.type(3)).isEqualTo(PointDataType.UINT8);
      while (reader.readBlock()) {
        for (int i = 0; i < reader.blockLength(); i++) {
          double x = (Double) reader.value(0, i);
          assertThat(x).isBetween(0.0, 100.5);
          assertThat(x).isGreaterThanOrEqualTo(lastX);
          lastX = x;
          assertThat((Long) reader.value(3, i)).isBetween(0L, 31L);
          points++;
        }
      }
    }
    assertThat(points).isEqualTo(1000);
  }

  @Test
  void rejectsUnknownDimensions(@TempDir Path tempDir) throws Exception {
    Path input = tempDir.resolve("blocks-input-2.laz");
    Pdal.execute(
        """
        {"pipeline":[
          {"type":"readers.faux","mode":"ramp","bounds":"([0,10],[0,10],[0,10])","count":10},
          {"type":"writers.las","filename":"%s"}]}
        """
            .formatted(jsonPath(input)));

    PdalPlan plan =
        PdalPlan.of(PdalStage.of("readers.las", Map.of("filename", input.toString())));
    assertThatThrownBy(() -> PdalBlockReader.open(plan, List.of("DoesNotExist"), 10))
        .hasMessageContaining("missing in point view");
  }

  private static String jsonPath(Path path) {
    return path.toString().replace("\\", "\\\\");
  }
}
