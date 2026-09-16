package ch.so.agi.hop.pdal.values;

import org.apache.hop.core.annotations.Transform;

@Transform(
    id = "SOGIS_POINTCLOUD_FILTER",
    name = "Point Cloud Filter",
    description = "Keep points matching an expression or a dimension range",
    image = "ch/so/agi/hop/pdal/values/icon.svg",
    categoryDescription = "Geospatial",
    classLoaderGroup = "sogeo-pointcloud",
    documentationUrl = "https://github.com/edigonzales/hop-pdal-plugin/blob/main/docs/transforms/point-cloud-filter.adoc#point-cloud-filter",
    keywords = {"point cloud", "lidar", "las", "laz", "copc", "pdal", "gis"})
public final class PointCloudFilterMeta extends PdalValueMeta {
  @Override
  public Operation operation() {
    return Operation.FILTER;
  }
}
