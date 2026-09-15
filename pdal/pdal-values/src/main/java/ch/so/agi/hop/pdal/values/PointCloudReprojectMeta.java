package ch.so.agi.hop.pdal.values;

import org.apache.hop.core.annotations.Transform;

@Transform(
    id = "SOGIS_POINTCLOUD_REPROJECT",
    name = "Point Cloud Reproject",
    description = "Plan a CRS transformation of a point cloud",
    image = "ch/so/agi/hop/pdal/values/icon.svg",
    categoryDescription = "Geospatial",
    classLoaderGroup = "sogeo-pointcloud",
    documentationUrl =
        "https://github.com/edigonzales/hop-pdal-plugin/blob/main/docs/transforms/point-cloud-reproject.adoc#point-cloud-reproject",
    keywords = {"point cloud", "lidar", "las", "laz", "copc", "pdal", "gis"})
public final class PointCloudReprojectMeta extends PdalValueMeta {
  @Override
  public Operation operation() {
    return Operation.REPROJECT;
  }
}
