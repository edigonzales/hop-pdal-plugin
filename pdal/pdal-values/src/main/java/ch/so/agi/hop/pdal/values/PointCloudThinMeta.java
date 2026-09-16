package ch.so.agi.hop.pdal.values;

import org.apache.hop.core.annotations.Transform;

@Transform(
    id = "SOGIS_POINTCLOUD_THIN",
    name = "Point Cloud Thin",
    description = "Thin a point cloud by decimation, voxel, grid, sampling or farthest point sampling",
    image = "ch/so/agi/hop/pdal/values/icon.svg",
    categoryDescription = "Geospatial",
    classLoaderGroup = "sogeo-pointcloud",
    documentationUrl = "https://github.com/edigonzales/hop-pdal-plugin/blob/main/docs/transforms/point-cloud-thin.adoc#point-cloud-thin",
    keywords = {"point cloud", "lidar", "las", "laz", "copc", "pdal", "gis"})
public final class PointCloudThinMeta extends PdalValueMeta {
  @Override
  public Operation operation() {
    return Operation.THIN;
  }
}
