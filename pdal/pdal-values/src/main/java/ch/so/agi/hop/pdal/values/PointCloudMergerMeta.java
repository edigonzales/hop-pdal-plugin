package ch.so.agi.hop.pdal.values;

import org.apache.hop.core.annotations.Transform;

@Transform(
    id = "SOGIS_POINTCLOUD_MERGER",
    name = "Point Cloud Merger",
    description = "Merge many point clouds into one pipeline and optionally batch them into groups",
    image = "ch/so/agi/hop/pdal/values/icon.svg",
    categoryDescription = "Geospatial",
    classLoaderGroup = "sogeo-pointcloud",
    documentationUrl = "https://github.com/edigonzales/hop-pdal-plugin/blob/main/docs/transforms/point-cloud-merger.adoc#point-cloud-merger",
    keywords = {"point cloud", "lidar", "las", "laz", "copc", "pdal", "gis"})
public final class PointCloudMergerMeta extends PdalValueMeta {
  @Override
  public Operation operation() {
    return Operation.MERGER;
  }
}
