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

  @Test
  void classifiesGroundAndComputesHeightAboveGround(@TempDir Path tempDir) throws Exception {
    Path input = tempDir.resolve("ground-input.laz");
    Path output = tempDir.resolve("ground-output.laz");

    Pdal.execute(
        """
        {"pipeline":[
          {"type":"readers.faux","mode":"ramp","bounds":"([0,100],[0,100],[0,10])","count":5000},
          {"type":"writers.las","filename":"%s"}]}
        """
            .formatted(jsonPath(input)));

    PdalBackend backend = new PdalBackend();
    PointCloudDescriptor descriptor = backend.describe(input.toString());
    PointCloudDataset dataset =
        PointCloudDataset.of(
            new PointCloudReference(input.toString(), null),
            descriptor,
            PdalPlan.of(PdalStage.of("readers.las", Map.of("filename", input.toString()))));

    var classified =
        dataset.append(
            PdalStage.of("filters.assign", Map.of("value", "Classification = 0")),
            descriptor.withPointCount(PointCloudDescriptor.UNKNOWN_POINT_COUNT));
    PdalPlan groundPlan =
        PdalOperationStages.ground(
                new PdalOperationStages.GroundOptions(
                    "SMRF", "1.25", "0.15", "0.5", "18", "", "", "", "", "", "", ""))
            .append(PdalOperationStages.hag("NN", "", "5", "", ""));
    var grounded = classified.append(groundPlan, classified.descriptor());

    PointCloudExecution execution =
        backend.execute(
            grounded
                .plan()
                .append(
                    PdalStage.of(
                        "writers.las",
                        Map.of("filename", output.toString(), "extra_dims", "all"))),
            null);
    assertThat(execution.pointCount()).isEqualTo(5000);

    PointCloudDescriptor outputDescriptor = backend.describe(output.toString());
    assertThat(outputDescriptor.dimensions())
        .anyMatch(d -> d.name().equals("HeightAboveGround"));

    var groundOnly =
        PointCloudDataset.of(
                new PointCloudReference(output.toString(), null),
                outputDescriptor,
                PdalPlan.of(PdalStage.of("readers.las", Map.of("filename", output.toString()))))
            .append(
                PdalStage.of("filters.range", Map.of("limits", "Classification[2:2]")),
                outputDescriptor.withPointCount(PointCloudDescriptor.UNKNOWN_POINT_COUNT));
    PointCloudExecution groundExecution =
        backend.execute(
            groundOnly.plan().append(PdalStage.of("writers.null", Map.of())), null);
    assertThat(groundExecution.pointCount()).isEqualTo(5000);
  }

  @Test
  void mergesTilesIntoOneCopcFile(@TempDir Path tempDir) throws Exception {
    Path first = tempDir.resolve("tile-1.laz");
    Path second = tempDir.resolve("tile-2.laz");
    Path output = tempDir.resolve("merged.copc.laz");
    Pdal.execute(
        """
        {"pipeline":[
          {"type":"readers.faux","mode":"ramp","bounds":"([0,1000],[0,1000],[0,100])","count":1000},
          {"type":"writers.las","filename":"%s"}]}
        """
            .formatted(jsonPath(first)));
    Pdal.execute(
        """
        {"pipeline":[
          {"type":"readers.faux","mode":"ramp","bounds":"([1000,2000],[0,1000],[0,100])","count":1000},
          {"type":"writers.las","filename":"%s"}]}
        """
            .formatted(jsonPath(second)));

    PdalBackend backend = new PdalBackend();
    var datasets =
        List.of(
            PointCloudDataset.of(
                new PointCloudReference(first.toString(), null),
                backend.describe(first.toString()),
                PdalPlan.of(PdalStage.of("readers.las", Map.of("filename", first.toString())))),
            PointCloudDataset.of(
                new PointCloudReference(second.toString(), null),
                backend.describe(second.toString()),
                PdalPlan.of(PdalStage.of("readers.las", Map.of("filename", second.toString())))));

    PointCloudDataset merged =
        PointCloudDataset.of(
            datasets.get(0).source(),
            PdalOperationStages.mergeDescriptor(datasets),
            PdalOperationStages.mergePlan(datasets));
    assertThat(merged.descriptor().pointCount()).isEqualTo(2000);

    PointCloudExecution execution =
        backend.execute(
            merged
                .plan()
                .append(
                    PdalStage.of(
                        "writers.copc",
                        Map.of("filename", output.toString(), "extra_dims", "all"))),
            null);
    assertThat(execution.pointCount()).isEqualTo(2000);
    assertThat(output).exists();

    PointCloudDescriptor copc = backend.describe(output.toString());
    assertThat(copc.pointCount()).isEqualTo(2000);
    assertThat(copc.bounds().minX()).isZero();
    assertThat(copc.bounds().maxX()).isBetween(1999.0, 2000.0);
  }

  private static String jsonPath(Path path) {
    return path.toString().replace("\\", "\\\\");
  }
}
