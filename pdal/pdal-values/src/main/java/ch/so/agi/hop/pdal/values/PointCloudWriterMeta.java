package ch.so.agi.hop.pdal.values;

import org.apache.hop.core.annotations.Transform;

@Transform(
    id = "SOGIS_POINTCLOUD_WRITER",
    name = "Point Cloud Writer",
    description = "Execute the planned pipeline once and write the result to LAS/LAZ, COPC or text",
    image = "ch/so/agi/hop/pdal/values/icon.svg",
    categoryDescription = "Geospatial",
    classLoaderGroup = "sogeo-pointcloud",
    documentationUrl =
        "https://github.com/edigonzales/hop-pdal-plugin/blob/main/docs/transforms/point-cloud-writer.adoc#point-cloud-writer",
    keywords = {"point cloud", "lidar", "las", "laz", "copc", "pdal", "gis"})
public final class PointCloudWriterMeta extends PdalValueMeta {
  @Override
  public Operation operation() {
    return Operation.WRITER;
  }
}
