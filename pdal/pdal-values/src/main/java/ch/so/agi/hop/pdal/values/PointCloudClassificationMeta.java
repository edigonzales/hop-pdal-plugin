package ch.so.agi.hop.pdal.values;

import org.apache.hop.core.annotations.Transform;

@Transform(
    id = "SOGIS_POINTCLOUD_CLASSIFICATION",
    name = "Point Cloud Classification",
    description = "Set the Classification dimension, optionally only for unclassified points",
    image = "ch/so/agi/hop/pdal/values/icon.svg",
    categoryDescription = "Geospatial",
    classLoaderGroup = "sogeo-pointcloud",
    documentationUrl = "https://github.com/edigonzales/hop-pdal-plugin/blob/main/docs/transforms/point-cloud-classification.adoc#point-cloud-classification",
    keywords = {"point cloud", "lidar", "las", "laz", "copc", "pdal", "gis"})
public final class PointCloudClassificationMeta extends PdalValueMeta {
  @Override
  public Operation operation() {
    return Operation.CLASSIFICATION;
  }
}
