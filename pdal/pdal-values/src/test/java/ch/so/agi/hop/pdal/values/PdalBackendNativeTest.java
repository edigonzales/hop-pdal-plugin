package ch.so.agi.hop.pdal.values;

import static org.assertj.core.api.Assertions.*;

import ch.so.agi.hop.pointcloud.*;
import ch.so.agi.pdal.ffm.Pdal;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration test against the bundled PDAL runtime. Enable with
 * {@code PDAL_RUN_INTEGRATION=true}; requires the platform natives artifact.
 */
@EnabledIfEnvironmentVariable(named = "PDAL_RUN_INTEGRATION", matches = "true")
class PdalBackendNativeTest {

  @Test
  void describesAndExecutesCroppedPipeline(@TempDir Path tempDir) throws Exception {
    Path input = tempDir.resolve("input.laz");
    Path output = tempDir.resolve("output.laz");

    Pdal.execute(
        """
        {"pipeline":[
          {"type":"readers.faux","mode":"ramp","bounds":"([0,100],[0,100],[0,10])","count":1000},
          {"type":"writers.las","filename":"%s"}]}
        """
            .formatted(jsonPath(input)));

    PdalBackend backend = new PdalBackend();
    PointCloudDescriptor descriptor = backend.describe(input.toString());

    assertThat(descriptor.pointCount()).isEqualTo(1000);
    assertThat(descriptor.bounds().maxX()).isBetween(99.0, 100.0);
    assertThat(descriptor.dimensions())
        .anyMatch(d -> d.name().equals("X") && d.type() == PointDataType.FLOAT64)
        .anyMatch(d -> d.name().equals("Intensity") && d.type() == PointDataType.UINT16);

    PointCloudDataset dataset =
        PointCloudDataset.of(
            new PointCloudReference(input.toString(), null),
            descriptor,
            PdalPlan.of(PdalStage.of("readers.las", Map.of("filename", input.toString()))));

    PointCloudDataset cropped =
        dataset.append(
            PdalStage.of("filters.crop", Map.of("bounds", "([0,50],[0,50])")),
            descriptor.withPointCount(PointCloudDescriptor.UNKNOWN_POINT_COUNT));

    PointCloudExecution execution =
        backend.execute(
            cropped
                .plan()
                .append(PdalStage.of("writers.las", Map.of("filename", output.toString()))),
            null);

    // The faux ramp correlates X and Y, so a quarter area keeps half of the points.
    assertThat(execution.pointCount()).isBetween(499L, 501L);
    assertThat(execution.metadataJson()).contains("writers.las");
    assertThat(output).exists();
  }

  private static String jsonPath(Path path) {
    return path.toString().replace("\\", "\\\\");
  }
}
