package ch.so.agi.hop.pdal.values;

import org.apache.hop.core.annotations.Transform;

@Transform(
    id = "SOGIS_POINTCLOUD_INFO",
    name = "Point Cloud Info",
    description = "Add point cloud metadata such as point count, bounds, CRS and dimensions",
    image = "ch/so/agi/hop/pdal/values/icon.svg",
    categoryDescription = "Geospatial",
    classLoaderGroup = "sogeo-pointcloud",
    documentationUrl =
        "https://github.com/edigonzales/hop-pdal-plugin/blob/main/docs/transforms/point-cloud-info.adoc#point-cloud-info",
    keywords = {"point cloud", "lidar", "las", "laz", "copc", "pdal", "gis"})
public final class PointCloudInfoMeta extends PdalValueMeta {
  @Override
  public Operation operation() {
    return Operation.INFO;
  }
}
