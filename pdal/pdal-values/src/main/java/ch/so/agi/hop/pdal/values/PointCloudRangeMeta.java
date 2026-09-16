package ch.so.agi.hop.pdal.values;

import org.apache.hop.core.annotations.Transform;

@Transform(
    id = "SOGIS_POINTCLOUD_RANGE",
    name = "Point Cloud Range",
    description = "Keep points within dimension ranges such as Z[400:900] or Classification[2:2]",
    image = "ch/so/agi/hop/pdal/values/icon.svg",
    categoryDescription = "Geospatial",
    classLoaderGroup = "sogeo-pointcloud",
    documentationUrl = "https://github.com/edigonzales/hop-pdal-plugin/blob/main/docs/transforms/point-cloud-range.adoc#point-cloud-range",
    keywords = {"point cloud", "lidar", "las", "laz", "copc", "pdal", "gis"})
public final class PointCloudRangeMeta extends PdalValueMeta {
  @Override
  public Operation operation() {
    return Operation.RANGE;
  }
}
