package ch.so.agi.hop.pdal.values;

import org.apache.hop.core.annotations.Transform;

@Transform(
    id = "SOGIS_POINTCLOUD_RAW_PIPELINE",
    name = "Point Cloud Raw Pipeline",
    description = "Append or replace pipeline stages with raw PDAL stage JSON (expert mode)",
    image = "ch/so/agi/hop/pdal/values/icon.svg",
    categoryDescription = "Geospatial",
    classLoaderGroup = "sogeo-pointcloud",
    documentationUrl = "https://github.com/edigonzales/hop-pdal-plugin/blob/main/docs/transforms/point-cloud-raw-pipeline.adoc#point-cloud-raw-pipeline",
    keywords = {"point cloud", "lidar", "las", "laz", "copc", "pdal", "gis"})
public final class PointCloudRawPipelineMeta extends PdalValueMeta {
  @Override
  public Operation operation() {
    return Operation.RAW;
  }
}
