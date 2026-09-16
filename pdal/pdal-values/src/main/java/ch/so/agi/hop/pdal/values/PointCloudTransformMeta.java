package ch.so.agi.hop.pdal.values;

import org.apache.hop.core.annotations.Transform;

@Transform(
    id = "SOGIS_POINTCLOUD_TRANSFORM",
    name = "Point Cloud Transform",
    description = "Apply a 4x4 transformation matrix to all points",
    image = "ch/so/agi/hop/pdal/values/icon.svg",
    categoryDescription = "Geospatial",
    classLoaderGroup = "sogeo-pointcloud",
    documentationUrl = "https://github.com/edigonzales/hop-pdal-plugin/blob/main/docs/transforms/point-cloud-transform.adoc#point-cloud-transform",
    keywords = {"point cloud", "lidar", "las", "laz", "copc", "pdal", "gis"})
public final class PointCloudTransformMeta extends PdalValueMeta {
  @Override
  public Operation operation() {
    return Operation.TRANSFORM;
  }
}
