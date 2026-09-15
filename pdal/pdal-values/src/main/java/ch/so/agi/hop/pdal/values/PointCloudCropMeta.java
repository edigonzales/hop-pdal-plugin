package ch.so.agi.hop.pdal.values;

import org.apache.hop.core.annotations.Transform;

@Transform(
    id = "SOGIS_POINTCLOUD_CROP",
    name = "Point Cloud Crop",
    description = "Plan a bounds crop of a point cloud",
    image = "ch/so/agi/hop/pdal/values/icon.svg",
    categoryDescription = "Geospatial",
    classLoaderGroup = "sogeo-pointcloud",
    documentationUrl =
        "https://github.com/edigonzales/hop-pdal-plugin/blob/main/docs/transforms/point-cloud-crop.adoc#point-cloud-crop",
    keywords = {"point cloud", "lidar", "las", "laz", "copc", "pdal", "gis"})
public final class PointCloudCropMeta extends PdalValueMeta {
  @Override
  public Operation operation() {
    return Operation.CROP;
  }
}
