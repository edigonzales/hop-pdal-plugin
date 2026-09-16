package ch.so.agi.hop.pdal.values;

import org.apache.hop.core.annotations.Transform;

@Transform(
    id = "SOGIS_POINTCLOUD_GROUND",
    name = "Point Cloud Ground",
    description = "Classify ground with SMRF, PMF or CSF and optionally compute height above ground",
    image = "ch/so/agi/hop/pdal/values/icon.svg",
    categoryDescription = "Geospatial",
    classLoaderGroup = "sogeo-pointcloud",
    documentationUrl = "https://github.com/edigonzales/hop-pdal-plugin/blob/main/docs/transforms/point-cloud-ground.adoc#point-cloud-ground",
    keywords = {"point cloud", "lidar", "las", "laz", "copc", "pdal", "gis"})
public final class PointCloudGroundMeta extends PdalValueMeta {
  @Override
  public Operation operation() {
    return Operation.GROUND;
  }
}
