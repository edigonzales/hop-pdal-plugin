package ch.so.agi.hop.pdal.values;

import org.apache.hop.core.annotations.Transform;

@Transform(
    id = "SOGIS_POINTCLOUD_STATISTICS",
    name = "Point Cloud Statistics",
    description = "Compute per dimension statistics (count, min, max, mean, stddev, variance, skewness, kurtosis)",
    image = "ch/so/agi/hop/pdal/values/icon.svg",
    categoryDescription = "Geospatial",
    classLoaderGroup = "sogeo-pointcloud",
    documentationUrl = "https://github.com/edigonzales/hop-pdal-plugin/blob/main/docs/transforms/point-cloud-statistics.adoc#point-cloud-statistics",
    keywords = {"point cloud", "lidar", "las", "laz", "copc", "pdal", "gis"})
public final class PointCloudStatisticsMeta extends PdalValueMeta {
  @Override
  public Operation operation() {
    return Operation.STATISTICS;
  }
}
