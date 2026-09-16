package ch.so.agi.hop.pdal.values;

import org.apache.hop.core.annotations.Transform;

@Transform(
    id = "SOGIS_POINTCLOUD_OUTLIER",
    name = "Point Cloud Outlier",
    description = "Remove outliers with statistical or radius based methods",
    image = "ch/so/agi/hop/pdal/values/icon.svg",
    categoryDescription = "Geospatial",
    classLoaderGroup = "sogeo-pointcloud",
    documentationUrl = "https://github.com/edigonzales/hop-pdal-plugin/blob/main/docs/transforms/point-cloud-outlier.adoc#point-cloud-outlier",
    keywords = {"point cloud", "lidar", "las", "laz", "copc", "pdal", "gis"})
public final class PointCloudOutlierMeta extends PdalValueMeta {
  @Override
  public Operation operation() {
    return Operation.OUTLIER;
  }
}
