package ch.so.agi.hop.pdal.values;

import ch.so.agi.hop.pointcloud.type.ValueMetaPointCloud;
import java.util.List;
import java.util.Locale;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.core.row.value.ValueMetaInteger;
import org.apache.hop.core.row.value.ValueMetaNumber;
import org.apache.hop.core.row.value.ValueMetaString;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.apache.hop.metadata.api.IHopMetadataProvider;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.BaseTransformMeta;
import org.apache.hop.pipeline.transform.ITransform;
import org.apache.hop.pipeline.transform.ITransformData;
import org.apache.hop.pipeline.transform.ITransformMeta;
import org.apache.hop.pipeline.transform.TransformMeta;

/**
 * Shared configuration of the point cloud transforms.
 *
 * <p>One meta class per operation only declares the operation; all properties live here so the
 * dialog and serialization stay in one place. The Hop metadata property names are stable and
 * versioned through {@code version}.
 */
public abstract class PdalValueMeta extends BaseTransformMeta<PdalValueTransform, PdalValueData> {

  public enum Operation {
    READER,
    INFO,
    CROP,
    REPROJECT,
    WRITER
  }

  public abstract Operation operation();

  @HopMetadataProperty private int version = 1;

  public int getVersion() {
    return version;
  }

  public void setVersion(int value) {
    version = value;
  }

  @HopMetadataProperty private String valueField = "pointcloud";

  public String getValueField() {
    return valueField;
  }

  public void setValueField(String value) {
    valueField = value;
  }

  @HopMetadataProperty private String outputValueField = "";

  public String getOutputValueField() {
    return outputValueField;
  }

  public void setOutputValueField(String value) {
    outputValueField = value;
  }

  @HopMetadataProperty private String source = "";

  public String getSource() {
    return source;
  }

  public void setSource(String value) {
    source = value;
  }

  @HopMetadataProperty private boolean sourceField = false;

  public boolean isSourceField() {
    return sourceField;
  }

  public void setSourceField(boolean value) {
    sourceField = value;
  }

  @HopMetadataProperty private String minX = "";

  public String getMinX() {
    return minX;
  }

  public void setMinX(String value) {
    minX = value;
  }

  @HopMetadataProperty private String minY = "";

  public String getMinY() {
    return minY;
  }

  public void setMinY(String value) {
    minY = value;
  }

  @HopMetadataProperty private String minZ = "";

  public String getMinZ() {
    return minZ;
  }

  public void setMinZ(String value) {
    minZ = value;
  }

  @HopMetadataProperty private String maxX = "";

  public String getMaxX() {
    return maxX;
  }

  public void setMaxX(String value) {
    maxX = value;
  }

  @HopMetadataProperty private String maxY = "";

  public String getMaxY() {
    return maxY;
  }

  public void setMaxY(String value) {
    maxY = value;
  }

  @HopMetadataProperty private String maxZ = "";

  public String getMaxZ() {
    return maxZ;
  }

  public void setMaxZ(String value) {
    maxZ = value;
  }

  @HopMetadataProperty private boolean boundsFields = false;

  public boolean isBoundsFields() {
    return boundsFields;
  }

  public void setBoundsFields(boolean value) {
    boundsFields = value;
  }

  @HopMetadataProperty private String targetCrs = "";

  public String getTargetCrs() {
    return targetCrs;
  }

  public void setTargetCrs(String value) {
    targetCrs = value;
  }

  @HopMetadataProperty private boolean targetCrsField = false;

  public boolean isTargetCrsField() {
    return targetCrsField;
  }

  public void setTargetCrsField(boolean value) {
    targetCrsField = value;
  }

  @HopMetadataProperty private String infoFields = "point_count,bounds,crs,dimensions";

  public String getInfoFields() {
    return infoFields;
  }

  public void setInfoFields(String value) {
    infoFields = value;
  }

  @HopMetadataProperty private String prefix = "";

  public String getPrefix() {
    return prefix;
  }

  public void setPrefix(String value) {
    prefix = value;
  }

  @HopMetadataProperty private boolean overwrite = false;

  public boolean isOverwrite() {
    return overwrite;
  }

  public void setOverwrite(boolean value) {
    overwrite = value;
  }

  @Override
  public void setDefault() {
    // Defaults are initialized in the field declarations.
  }

  @Override
  public String getDialogClassName() {
    return PdalValueDialog.class.getName();
  }

  @Override
  public ITransform createTransform(
      TransformMeta transformMeta,
      ITransformData data,
      int copyNr,
      PipelineMeta pipelineMeta,
      org.apache.hop.pipeline.Pipeline pipeline) {
    return new PdalValueTransform(
        transformMeta, this, (PdalValueData) data, copyNr, pipelineMeta, pipeline);
  }

  @Override
  public ITransformData createTransformData() {
    return new PdalValueData();
  }

  @Override
  public boolean supportsErrorHandling() {
    return true;
  }

  public void validateSettings() {
    if (version != 1) {
      throw new IllegalArgumentException(
          "Legacy point cloud configuration (value version 1 expected): migrate the transform");
    }
    if (valueField == null || valueField.isBlank()) {
      throw new IllegalArgumentException("Point Cloud field is required");
    }
    String operation = operation().name();
    if ((operation.equals("READER") || operation.equals("WRITER"))
        && (source == null || source.isBlank())) {
      throw new IllegalArgumentException(
          operation.equals("READER") ? "Point cloud source is required" : "Output file is required");
    }
    if (operation.equals("CROP")) {
      for (String bound : List.of(minX, minY, maxX, maxY)) {
        if (bound == null || bound.isBlank()) {
          throw new IllegalArgumentException("Crop bounds minX, minY, maxX and maxY are required");
        }
      }
    }
    if (operation.equals("REPROJECT") && !targetCrsField && (targetCrs == null || targetCrs.isBlank())) {
      throw new IllegalArgumentException("Target CRS is required");
    }
  }

  /** Field name the transform writes the resulting value to. */
  public String resultField(IVariables vars) {
    if (operation() == Operation.READER || outputValueField == null || outputValueField.isBlank()) {
      return vars.resolve(valueField);
    }
    return vars.resolve(outputValueField);
  }

  public List<String> selectedInfoFields() {
    return tokens(infoFields).stream().map(field -> field.toLowerCase(Locale.ROOT)).toList();
  }

  public static List<String> tokens(String value) {
    if (value == null || value.isBlank()) return List.of();
    return List.of(value.split(",")).stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
  }

  @Override
  public void getFields(
      IRowMeta row,
      String origin,
      IRowMeta[] info,
      TransformMeta next,
      IVariables vars,
      IHopMetadataProvider provider) {
    validateSettings();
    String operation = operation().name();
    String input = vars.resolve(valueField);

    if (!operation.equals("READER")) {
      int index = row.indexOfValue(input);
      if (index < 0 || row.getValueMeta(index).getType() != ValueMetaPointCloud.TYPE_POINT_CLOUD) {
        throw new IllegalArgumentException("Expected Point Cloud field: " + input);
      }
    }

    if (operation.equals("READER")) {
      add(row, origin, new ValueMetaPointCloud(input));
    } else if (operation.equals("CROP") || operation.equals("REPROJECT")) {
      String name = resultField(vars);
      if (!name.equals(input)) add(row, origin, new ValueMetaPointCloud(name));
    } else if (operation.equals("WRITER")) {
      add(row, origin, new ValueMetaString(vars.resolve(prefix) + "output_file"));
      add(row, origin, new ValueMetaString(vars.resolve(prefix) + "status"));
    } else if (operation.equals("INFO")) {
      for (String field : selectedInfoFields()) {
        String name = vars.resolve(prefix) + field;
        add(
            row,
            origin,
            switch (field) {
              case "point_count" -> new ValueMetaInteger(name);
              case "minx", "miny", "minz", "maxx", "maxy", "maxz" -> new ValueMetaNumber(name);
              case "bounds", "crs", "dimensions" -> new ValueMetaString(name);
              default -> throw new IllegalArgumentException("Unknown point cloud information: " + field);
            });
      }
    }
  }

  private static void add(IRowMeta row, String origin, IValueMeta value) {
    value.setOrigin(origin);
    row.addValueMeta(value);
  }
}
