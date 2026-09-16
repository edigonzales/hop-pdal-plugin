package ch.so.agi.hop.pdal.values;

import static org.assertj.core.api.Assertions.*;

import ch.so.agi.hop.pointcloud.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PdalOperationStagesTest {

  @Test
  void buildsExpressionStage() {
    PdalStage stage = PdalOperationStages.expression("Z > 500", false, "");
    assertThat(stage.type()).isEqualTo("filters.expression");
    assertThat(stage.options()).containsEntry("expression", "Z > 500");

    PdalStage inverted = PdalOperationStages.expression("Classification == 2", true, "Z > 400");
    assertThat(inverted.options())
        .containsEntry("expression", "!(Classification == 2)")
        .containsEntry("where", "Z > 400");
  }

  @Test
  void buildsRangeStage() {
    PdalStage stage = PdalOperationStages.range("Z[400:900]", "");
    assertThat(stage.type()).isEqualTo("filters.range");
    assertThat(stage.options()).containsEntry("limits", "Z[400:900]");
  }

  @Test
  void buildsAssignStages() {
    PdalStage value = PdalOperationStages.assign("VALUE", "Foo = Z * 2", "", "");
    assertThat(value.type()).isEqualTo("filters.assign");
    assertThat(value.options()).containsEntry("value", "Foo = Z * 2");

    PdalStage assignment =
        PdalOperationStages.assign("ASSIGNMENT", "Classification[:]=2", "Z[0:100]", "");
    assertThat(assignment.options())
        .containsEntry("assignment", "Classification[:]=2")
        .containsEntry("condition", "Z[0:100]");
  }

  @Test
  void buildsClassificationStage() {
    PdalStage stage = PdalOperationStages.classification("2", true, "Z > 400");
    assertThat(stage.type()).isEqualTo("filters.assign");
    assertThat(stage.options().get("value")).isEqualTo("Classification = 2");
    assertThat(stage.options().get("where")).isEqualTo("(Classification == 0) && (Z > 400)");
  }

  @Test
  void buildsThinStages() {
    assertThat(PdalOperationStages.thin("DECIMATION", "5", "", "", "", "", "", "").type())
        .isEqualTo("filters.decimation");
    var voxel = PdalOperationStages.thin("VOXEL_DOWNSIZE", "", "2.5", "center", "", "", "", "");
    assertThat(voxel.type()).isEqualTo("filters.voxeldownsize");
    assertThat(voxel.options()).containsEntry("cell", "2.5").containsEntry("mode", "center");
    var grid = PdalOperationStages.thin("GRID_DECIMATION", "", "", "", "1.0", "", "", "");
    assertThat(grid.type()).isEqualTo("filters.gridDecimation");
    assertThat(grid.options()).containsEntry("resolution", "1.0");
    var sample = PdalOperationStages.thin("SAMPLE", "", "", "", "", "3.0", "", "");
    assertThat(sample.type()).isEqualTo("filters.sample");
    var fps = PdalOperationStages.thin("FPS", "", "", "", "", "", "1000", "");
    assertThat(fps.type()).isEqualTo("filters.fps");
    assertThat(fps.options()).containsEntry("count", "1000");
  }

  @Test
  void buildsOutlierStages() {
    var statistical = PdalOperationStages.outlier("STATISTICAL", "8", "2.5", "", "2", "");
    assertThat(statistical.type()).isEqualTo("filters.outlier");
    assertThat(statistical.options())
        .containsEntry("method", "statistical")
        .containsEntry("mean_k", "8")
        .containsEntry("multiplier", "2.5");

    var radius = PdalOperationStages.outlier("RADIUS", "8", "2.5", "1.0", "3", "");
    assertThat(radius.options())
        .containsEntry("method", "radius")
        .containsEntry("radius", "1.0")
        .containsEntry("min_k", "3");
  }

  @Test
  void buildsSortStages() {
    var sort = PdalOperationStages.sort(false, false, "Z", true, "");
    assertThat(sort.type()).isEqualTo("filters.sort");
    assertThat(sort.options()).containsEntry("dimensions", "Z").containsEntry("order", "DESC");

    var morton = PdalOperationStages.sort(true, true, "", false, "");
    assertThat(morton.type()).isEqualTo("filters.mortonorder");
    assertThat(morton.options()).containsEntry("reverse", "true");
  }

  @Test
  void buildsTransformationStage() {
    var stage = PdalOperationStages.transformation("1 0 0 0 0 1 0 0 0 0 1 0 0 0 0 1", "");
    assertThat(stage.type()).isEqualTo("filters.transformation");
    assertThat(stage.options()).containsEntry("matrix", "1 0 0 0 0 1 0 0 0 0 1 0 0 0 0 1");
  }

  @Test
  void buildsGroundPlans() {
    var smrf =
        PdalOperationStages.ground(
            new PdalOperationStages.GroundOptions(
                "SMRF", "1.25", "0.15", "0.5", "18", "", "", "", "", "", "", ""));
    assertThat(smrf.stages()).hasSize(1);
    assertThat(smrf.stages().get(0).type()).isEqualTo("filters.smrf");
    assertThat(smrf.stages().get(0).options())
        .containsEntry("scalar", "1.25")
        .containsEntry("window", "18");

    var pmf =
        PdalOperationStages.ground(
            new PdalOperationStages.GroundOptions(
                "PMF", "", "0.15", "", "", "", "1.0", "0.15", "33", "", "", ""));
    assertThat(pmf.stages().get(0).type()).isEqualTo("filters.pmf");
    assertThat(pmf.stages().get(0).options())
        .containsEntry("cell_size", "1.0")
        .containsEntry("max_window_size", "33");

    var csf =
        PdalOperationStages.ground(
            new PdalOperationStages.GroundOptions(
                "CSF", "", "", "0.5", "", "", "", "", "", "1.0", "2", ""));
    assertThat(csf.stages().get(0).type()).isEqualTo("filters.csf");
    assertThat(csf.stages().get(0).options())
        .containsEntry("resolution", "1.0")
        .containsEntry("rigidness", "2");
  }

  @Test
  void buildsHagStages() {
    assertThat(PdalOperationStages.hag("NN", "", "10", "1.5", "").type())
        .isEqualTo("filters.hag_nn");
    assertThat(PdalOperationStages.hag("DELAUNAY", "", "", "", "").type())
        .isEqualTo("filters.hag_delaunay");
    var dem = PdalOperationStages.hag("DEM", "/data/dem.tif", "", "", "");
    assertThat(dem.type()).isEqualTo("filters.hag_dem");
    assertThat(dem.options()).containsEntry("raster", "/data/dem.tif");
  }

  @Test
  void parsesRawPipelines() {
    var stages =
        PdalOperationStages.parseRawStages(
            """
            {"pipeline":[
              {"type":"filters.crop","bounds":"([0,10],[0,10])"},
              {"type":"filters.decimation","step":5,"invert":false}]}
            """);
    assertThat(stages).hasSize(2);
    assertThat(stages.get(1).options()).containsEntry("step", "5").containsEntry("invert", "false");

    assertThat(PdalOperationStages.parseRawStages("[{\"type\":\"filters.head\",\"count\":100}]"))
        .hasSize(1);
  }

  @Test
  void rejectsInvalidRawPipelines() {
    assertThatThrownBy(() -> PdalOperationStages.parseRawStages(""))
        .hasMessageContaining("required");
    assertThatThrownBy(() -> PdalOperationStages.parseRawStages("{oops"))
        .hasMessageContaining("not valid JSON");
    assertThatThrownBy(() -> PdalOperationStages.parseRawStages("[{\"bounds\":\"x\"}]"))
        .hasMessageContaining("without 'type'");
    assertThatThrownBy(
            () -> PdalOperationStages.parseRawStages("[{\"type\":\"filters.crop\",\"vlrs\":{\"a\":1}}]"))
        .hasMessageContaining("scalar");
    assertThatThrownBy(
            () ->
                PdalOperationStages.raw(
                    "[{\"type\":\"filters.crop\",\"bounds\":\"([0,1],[0,1])\"}]", "REPLACE"))
        .hasMessageContaining("reader stage");
  }

  @Test
  void mergesPlansAndDescriptors() {
    var first = dataset("/data/a.laz", "EPSG:2056", 100, 0, 0, 0, 10, 10, 5);
    var second = dataset("/data/b.laz", "EPSG:2056", 50, 10, 10, 0, 20, 20, 5);

    PdalPlan plan = PdalOperationStages.mergePlan(List.of(first, second));
    assertThat(plan.size()).isEqualTo(3);
    assertThat(plan.stages().get(2).type()).isEqualTo("filters.merge");

    PointCloudDescriptor descriptor =
        PdalOperationStages.mergeDescriptor(List.of(first, second));
    assertThat(descriptor.pointCount()).isEqualTo(150);
    assertThat(descriptor.authority()).isEqualTo("EPSG:2056");
    assertThat(descriptor.bounds().minX()).isZero();
    assertThat(descriptor.bounds().maxX()).isEqualTo(20);
    assertThat(descriptor.dimensions()).hasSize(3);
  }

  @Test
  void rejectsIncompatibleMerges() {
    var first = dataset("/data/a.laz", "EPSG:2056", 100, 0, 0, 0, 10, 10, 5);
    var otherCrs = dataset("/data/b.laz", "EPSG:4326", 50, 0, 0, 0, 1, 1, 1);
    assertThatThrownBy(() -> PdalOperationStages.mergeDescriptor(List.of(first, otherCrs)))
        .hasMessageContaining("mismatch");

    var filtered =
        first.append(
            PdalStage.of("filters.crop", Map.of("bounds", "([0,1],[0,1])")),
            first.descriptor().withPointCount(PointCloudDescriptor.UNKNOWN_POINT_COUNT));
    assertThatThrownBy(() -> PdalOperationStages.mergePlan(List.of(filtered)))
        .hasMessageContaining("unprocessed");
  }

  @Test
  void unknownCountStaysUnknownAfterMerge() {
    var first = dataset("/data/a.laz", "EPSG:2056", 100, 0, 0, 0, 10, 10, 5);
    var second =
        new PointCloudDataset(
            first.source(),
            first.descriptor().withPointCount(PointCloudDescriptor.UNKNOWN_POINT_COUNT),
            first.plan());

    PointCloudDescriptor descriptor =
        PdalOperationStages.mergeDescriptor(List.of(first, second));
    assertThat(descriptor.hasPointCount()).isFalse();
  }

  private static PointCloudDataset dataset(
      String location,
      String authority,
      long pointCount,
      double minX,
      double minY,
      double minZ,
      double maxX,
      double maxY,
      double maxZ) {
    return dataset(
        location, "readers.las", authority, pointCount, minX, minY, minZ, maxX, maxY, maxZ);
  }

  private static PointCloudDataset dataset(
      String location,
      String readerType,
      String authority,
      long pointCount,
      double minX,
      double minY,
      double minZ,
      double maxX,
      double maxY,
      double maxZ) {
    return PointCloudDataset.of(
        new PointCloudReference(location, null),
        new PointCloudDescriptor(
            null,
            authority,
            pointCount,
            new PointCloudBounds(minX, minY, minZ, maxX, maxY, maxZ),
            List.of(
                PointDimension.of("X", PointDataType.FLOAT64),
                PointDimension.of("Y", PointDataType.FLOAT64),
                PointDimension.of("Z", PointDataType.FLOAT64))),
        PdalPlan.of(PdalStage.of(readerType, Map.of("filename", location))));
  }

  @Test
  void pushesCropBoundsIntoRemoteCopcReaders() {
    var remote =
        dataset(
            "https://example.org/tile.copc.laz",
            "readers.copc",
            "EPSG:2056",
            1000,
            2600000,
            1200000,
            400,
            2601000,
            1201000,
            900);

    PointCloudDataset clipped =
        PdalOperationStages.remoteReaderClip(remote, 2600200, 1200200, 400, 2600400, 1200400, 900);

    assertThat(clipped).isNotSameAs(remote);
    assertThat(clipped.plan().stages().get(0).type()).isEqualTo("readers.copc");
    assertThat(clipped.plan().stages().get(0).option("bounds"))
        .isEqualTo("([2600200.0,2600400.0],[1200200.0,1200400.0],[400.0,900.0])");
    assertThat(clipped.plan().stages().get(0).option("filename"))
        .isEqualTo("https://example.org/tile.copc.laz");

    // Local files and non-octree readers stay untouched.
    var local = dataset("/data/tile.laz", "EPSG:2056", 10, 0, 0, 0, 1, 1, 1);
    assertThat(PdalOperationStages.remoteReaderClip(local, 0, 0, 0, 1, 1, 1)).isSameAs(local);
    var remoteLas =
        dataset(
            "https://example.org/tile.laz",
            "readers.las",
            "EPSG:2056",
            10,
            0,
            0,
            0,
            1,
            1,
            1);
    assertThat(PdalOperationStages.remoteReaderClip(remoteLas, 0, 0, 0, 1, 1, 1))
        .isSameAs(remoteLas);
  }
}
