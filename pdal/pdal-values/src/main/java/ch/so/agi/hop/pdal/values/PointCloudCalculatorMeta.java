package ch.so.agi.hop.pdal.values;

import org.apache.hop.core.annotations.Transform;

@Transform(
    id = "SOGIS_POINTCLOUD_CALCULATOR",
    name = "Point Cloud Calculator",
    description = "Calculate dimensions with value, range or condition assignments (for example Foo = Z * 2)",
    image = "ch/so/agi/hop/pdal/values/icon.svg",
    categoryDescription = "Geospatial",
    classLoaderGroup = "sogeo-pointcloud",
    documentationUrl = "https://github.com/edigonzales/hop-pdal-plugin/blob/main/docs/transforms/point-cloud-calculator.adoc#point-cloud-calculator",
    keywords = {"point cloud", "lidar", "las", "laz", "copc", "pdal", "gis"})
public final class PointCloudCalculatorMeta extends PdalValueMeta {
  @Override
  public Operation operation() {
    return Operation.CALCULATOR;
  }
}
