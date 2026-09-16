package ch.so.agi.hop.pdal.values;

import org.apache.hop.core.annotations.Transform;

@Transform(
    id = "SOGIS_POINTCLOUD_SORT",
    name = "Point Cloud Sort",
    description = "Sort points by dimensions or Morton code",
    image = "ch/so/agi/hop/pdal/values/icon.svg",
    categoryDescription = "Geospatial",
    classLoaderGroup = "sogeo-pointcloud",
    documentationUrl = "https://github.com/edigonzales/hop-pdal-plugin/blob/main/docs/transforms/point-cloud-sort.adoc#point-cloud-sort",
    keywords = {"point cloud", "lidar", "las", "laz", "copc", "pdal", "gis"})
public final class PointCloudSortMeta extends PdalValueMeta {
  @Override
  public Operation operation() {
    return Operation.SORT;
  }
}
