package ch.so.agi.hop.pdal.values;

import org.apache.hop.core.annotations.Transform;

@Transform(
    id = "SOGIS_POINTCLOUD_TO_ROWS",
    name = "Point Cloud to Rows",
    description = "Execute a point cloud and emit one Hop row per point with block-wise dimension reads",
    image = "ch/so/agi/hop/pdal/values/icon.svg",
    categoryDescription = "Geospatial",
    classLoaderGroup = "sogeo-pointcloud",
    documentationUrl = "https://github.com/edigonzales/hop-pdal-plugin/blob/main/docs/transforms/point-cloud-to-rows.adoc#point-cloud-to-rows",
    keywords = {"point cloud", "lidar", "las", "laz", "copc", "pdal", "gis"})
public final class PointCloudToRowsMeta extends PdalValueMeta {
  @Override
  public Operation operation() {
    return Operation.TO_ROWS;
  }
}
