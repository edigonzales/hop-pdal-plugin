package ch.so.agi.hop.pdal.values;

import static org.assertj.core.api.Assertions.*;

import ch.so.agi.hop.pointcloud.type.ValueMetaPointCloud;
import org.apache.hop.core.row.RowMeta;
import org.apache.hop.core.variables.Variables;
import org.junit.jupiter.api.Test;

class PdalValueMetaTest {

  @Test
  void readerAddsPointCloudField() {
    var meta = new PointCloudReaderMeta();
    meta.setValueField("pc");
    meta.setSource("/data/tile.laz");

    var row = new RowMeta();
    meta.getFields(row, "reader", null, null, new Variables(), null);

    assertThat(row.searchValueMeta("pc")).isNotNull();
    assertThat(row.searchValueMeta("pc").getType()).isEqualTo(ValueMetaPointCloud.TYPE_POINT_CLOUD);
    assertThat(meta.resultField(new Variables())).isEqualTo("pc");
  }

  @Test
  void cropAddsOutputFieldOnlyWhenRenamed() {
    var row = new RowMeta();
    row.addValueMeta(new ValueMetaPointCloud("pc"));

    var inPlace = new PointCloudCropMeta();
    inPlace.setValueField("pc");
    inPlace.setMinX("0");
    inPlace.setMinY("0");
    inPlace.setMaxX("10");
    inPlace.setMaxY("10");
    inPlace.getFields(row, "crop", null, null, new Variables(), null);
    assertThat(row.size()).isEqualTo(1);

    var renamed = new PointCloudCropMeta();
    renamed.setValueField("pc");
    renamed.setOutputValueField("cropped");
    renamed.setMinX("0");
    renamed.setMinY("0");
    renamed.setMaxX("10");
    renamed.setMaxY("10");
    renamed.getFields(row, "crop", null, null, new Variables(), null);
    assertThat(row.searchValueMeta("cropped")).isNotNull();
  }

  @Test
  void infoAddsSelectedFields() {
    var row = new RowMeta();
    row.addValueMeta(new ValueMetaPointCloud("pc"));

    var meta = new PointCloudInfoMeta();
    meta.setValueField("pc");
    meta.setInfoFields("point_count,minx,crs");
    meta.setPrefix("info_");
    meta.getFields(row, "info", null, null, new Variables(), null);

    assertThat(row.searchValueMeta("info_point_count")).isNotNull();
    assertThat(row.searchValueMeta("info_minx")).isNotNull();
    assertThat(row.searchValueMeta("info_crs")).isNotNull();
    assertThat(meta.selectedInfoFields()).containsExactly("point_count", "minx", "crs");
  }

  @Test
  void writerAddsStatusFields() {
    var row = new RowMeta();
    row.addValueMeta(new ValueMetaPointCloud("pc"));

    var meta = new PointCloudWriterMeta();
    meta.setValueField("pc");
    meta.setSource("/data/out.laz");
    meta.getFields(row, "writer", null, null, new Variables(), null);

    assertThat(row.searchValueMeta("output_file")).isNotNull();
    assertThat(row.searchValueMeta("status")).isNotNull();
  }

  @Test
  void validatesSettings() {
    var reader = new PointCloudReaderMeta();
    assertThatThrownBy(reader::validateSettings).hasMessageContaining("source");

    var crop = new PointCloudCropMeta();
    crop.setMinX("0");
    crop.setMinY("0");
    crop.setMaxX("10");
    assertThatThrownBy(crop::validateSettings).hasMessageContaining("maxY");

    var reproject = new PointCloudReprojectMeta();
    assertThatThrownBy(reproject::validateSettings).hasMessageContaining("CRS");

    var writer = new PointCloudWriterMeta();
    assertThatThrownBy(writer::validateSettings).hasMessageContaining("Output file");

    var legacy = new PointCloudReaderMeta();
    legacy.setVersion(2);
    legacy.setSource("/data/tile.laz");
    assertThatThrownBy(legacy::validateSettings).hasMessageContaining("Legacy");
  }

  @Test
  void mergerClearsRowAndKeepsGroupAndValue() {
    var row = new RowMeta();
    row.addValueMeta(new org.apache.hop.core.row.value.ValueMetaString("tile"));
    row.addValueMeta(new ValueMetaPointCloud("pointcloud"));
    row.addValueMeta(new org.apache.hop.core.row.value.ValueMetaString("unrelated"));

    var meta = new PointCloudMergerMeta();
    meta.setValueField("pointcloud");
    meta.setGroupField("tile");
    meta.getFields(row, "merger", null, null, new Variables(), null);

    assertThat(row.size()).isEqualTo(2);
    assertThat(row.searchValueMeta("tile")).isNotNull();
    assertThat(row.searchValueMeta("pointcloud")).isNotNull();
    assertThat(row.searchValueMeta("unrelated")).isNull();
  }

  @Test
  void validatesNewOperations() {
    var filter = new PointCloudFilterMeta();
    assertThatThrownBy(filter::validateSettings).hasMessageContaining("expression");
    filter.setExpression("Z > 1");
    filter.validateSettings();

    var range = new PointCloudRangeMeta();
    assertThatThrownBy(range::validateSettings).hasMessageContaining("limits");

    var ground = new PointCloudGroundMeta();
    ground.setComputeHag(true);
    ground.setHagType("DEM");
    assertThatThrownBy(ground::validateSettings).hasMessageContaining("DEM");

    var raw = new PointCloudRawPipelineMeta();
    assertThatThrownBy(raw::validateSettings).hasMessageContaining("Raw pipeline");
  }

  @Test
  void parsesAssignedDimensions() {
    assertThat(PdalValueTransform.assignedDimensions("Foo = Z * 2"))
        .containsExactly("Foo");
    assertThat(PdalValueTransform.assignedDimensions("Classification[:]=2, Height = Z"))
        .containsExactly("Classification", "Height");
    assertThat(PdalValueTransform.assignedDimensions("")).isEmpty();
  }

  @Test
  void readerValidatesResolutionSupport() {
    var meta = new PointCloudReaderMeta();
    meta.setSource("/data/tile.laz");
    meta.setResolution("10");
    // Resolution is validated at runtime against the reader type.
    meta.validateSettings();
    assertThat(meta.getResolution()).isEqualTo("10");
  }
}
