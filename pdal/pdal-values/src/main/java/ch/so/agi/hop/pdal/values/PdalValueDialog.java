package ch.so.agi.hop.pdal.values;

import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.ui.core.PropsUi;
import org.apache.hop.ui.core.widget.ComboVar;
import org.apache.hop.ui.core.widget.TextVar;
import org.apache.hop.ui.pipeline.transform.BaseTransformDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/** Operation specific fields, edited on a clone so Cancel cannot change the pipeline. */
public final class PdalValueDialog extends BaseTransformDialog {
  private final PdalValueMeta input;

  public PdalValueDialog(
      Shell parent, IVariables variables, PdalValueMeta meta, PipelineMeta pipelineMeta) {
    super(parent, variables, meta, pipelineMeta);
    input = meta;
  }

  @Override
  public String open() {
    shell = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.RESIZE);
    shell.setText("Point Cloud " + titleCase(input.operation().name()));
    shell.setLayout(new GridLayout(1, false));
    PropsUi.setLook(shell);
    setShellImage(shell, input);

    var scroll = new ScrolledComposite(shell, SWT.V_SCROLL);
    scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    scroll.setExpandHorizontal(true);
    scroll.setExpandVertical(true);

    var body = new Composite(scroll, SWT.NONE);
    body.setLayout(new GridLayout(2, false));

    new Label(body, SWT.NONE).setText("Transform name");
    wTransformName = new Text(body, SWT.BORDER);
    wTransformName.setText(transformName);
    wTransformName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    var controls = new LinkedHashMap<String, Control>();
    String fields =
        switch (input.operation()) {
          case READER -> "source sourceField driver valueField overrideSrs resolution";
          case INFO -> "valueField infoFields prefix";
          case CROP -> "valueField outputValueField boundsFields minX minY minZ maxX maxY maxZ";
          case REPROJECT -> "valueField outputValueField targetCrs targetCrsField";
          case WRITER ->
              "valueField source sourceField overwrite compression minorVersion extraDims overrideSrs threads forward prefix";
          case FILTER -> "valueField outputValueField expression invertFilter where";
          case RANGE -> "valueField outputValueField limits where";
          case CALCULATOR -> "valueField outputValueField assignmentKind assignment condition where";
          case CLASSIFICATION -> "valueField outputValueField classification onlyUnclassified where";
          case THIN ->
              "valueField outputValueField thinType step voxelCell voxelMode gridCell sampleRadius fpsCount where";
          case OUTLIER -> "valueField outputValueField outlierType meanK multiplier radius minK where";
          case SORT -> "valueField outputValueField mortonOrder mortonReverse sortDimensions sortDescending where";
          case TRANSFORM -> "valueField outputValueField matrix where";
          case GROUND ->
              "valueField outputValueField groundType scalar slope threshold window groundCell pmfCellSize initialDistance maxWindowSize csfResolution csfRigidness computeHag hagType hagDemFile hagCount hagMaxDistance where";
          case RAW -> "valueField outputValueField rawMode rawPipeline";
          case STATISTICS ->
              "valueField outputValueField statsDimensions statsFields prefix";
          case TO_ROWS -> "valueField rowDimensions prefix maxPoints";
          case MERGER -> "valueField outputValueField groupField batchSize prefix";
        };

    try {
      for (String name : fields.split(" ")) {
        if (name.isEmpty()) continue;
        var field = PdalValueMeta.class.getDeclaredField(name);
        field.setAccessible(true);
        new Label(body, SWT.NONE).setText(label(name));
        Control control;
        if (field.getType() == boolean.class) {
          var button = new Button(body, SWT.CHECK);
          button.setSelection(field.getBoolean(input));
          control = button;
        } else if (isCombo(name)) {
          var combo = new ComboVar(variables, body, SWT.BORDER);
          String[] choices = explicitChoices(name);
          if (choices.length == 0) {
            try {
              choices =
                  pipelineMeta.getPrevTransformFields(variables, transformName).getFieldNames();
            } catch (Exception ignored) {
              // No previous transforms yet.
            }
          }
          combo.setItems(choices);
          combo.setText(String.valueOf(field.get(input)));
          combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
          control = combo;
        } else {
          var text = new TextVar(variables, body, SWT.BORDER);
          text.setText(String.valueOf(field.get(input)));
          text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
          control = text;
        }
        controls.put(name, control);
      }
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }

    scroll.setContent(body);
    scroll.setMinSize(body.computeSize(SWT.DEFAULT, SWT.DEFAULT));

    var buttons = new Composite(shell, SWT.NONE);
    buttons.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
    buttons.setLayout(new GridLayout(2, true));
    var ok = new Button(buttons, SWT.PUSH);
    ok.setText("OK");
    var cancel = new Button(buttons, SWT.PUSH);
    cancel.setText("Cancel");

    ok.addListener(
        SWT.Selection,
        event -> {
          try {
            var edited = (PdalValueMeta) input.clone();
            for (var entry : controls.entrySet()) {
              var field = PdalValueMeta.class.getDeclaredField(entry.getKey());
              field.setAccessible(true);
              Object value =
                  entry.getValue() instanceof Button button
                      ? button.getSelection()
                      : entry.getValue() instanceof ComboVar combo
                          ? combo.getText()
                          : ((TextVar) entry.getValue()).getText();
              if (field.getType() == int.class) value = Integer.parseInt((String) value);
              field.set(edited, value);
            }
            edited.validateSettings();
            if (wTransformName.getText().isBlank()) {
              throw new IllegalArgumentException("Transform name is required");
            }
            for (String name : controls.keySet()) {
              var field = PdalValueMeta.class.getDeclaredField(name);
              field.setAccessible(true);
              field.set(input, field.get(edited));
            }
            transformName = wTransformName.getText();
            input.setChanged();
            shell.dispose();
          } catch (Exception e) {
            var box = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
            box.setText("Point cloud settings");
            box.setMessage(e.getMessage());
            box.open();
          }
        });

    cancel.addListener(
        SWT.Selection,
        event -> {
          transformName = null;
          shell.dispose();
        });

    shell.setDefaultButton(ok);
    shell.pack();
    shell.open();
    var display = shell.getDisplay();
    while (!shell.isDisposed()) {
      if (!display.readAndDispatch()) display.sleep();
    }
    return transformName;
  }

  private static boolean isCombo(String name) {
    return switch (name) {
      case "valueField",
              "outputValueField",
              "infoFields",
              "prefix",
              "assignmentKind",
              "thinType",
              "outlierType",
              "groundType",
              "hagType",
              "rawMode",
              "driver",
              "voxelMode",
              "groupField" -> true;
      default -> false;
    };
  }

  private static String[] explicitChoices(String name) {
    return switch (name) {
      case "assignmentKind" -> new String[] {"VALUE", "ASSIGNMENT"};
      case "thinType" ->
          new String[] {"DECIMATION", "VOXEL_DOWNSIZE", "GRID_DECIMATION", "SAMPLE", "FPS"};
      case "outlierType" -> new String[] {"STATISTICAL", "RADIUS"};
      case "groundType" -> new String[] {"SMRF", "PMF", "CSF"};
      case "hagType" -> new String[] {"NN", "DELAUNAY", "DEM"};
      case "rawMode" -> new String[] {"APPEND", "REPLACE"};
      case "voxelMode" -> new String[] {"first", "center", "centroid"};
      case "driver" ->
          new String[] {"", "las", "copc", "ept", "bpf", "ply", "text", "gdal", "pcd", "e57"};
      default -> new String[0];
    };
  }

  private static String label(String name) {
    var label = new StringBuilder();
    for (char c : name.toCharArray()) {
      if (Character.isUpperCase(c) && label.length() > 0) label.append(' ');
      label.append(Character.toLowerCase(c));
    }
    return Character.toUpperCase(label.charAt(0)) + label.substring(1);
  }

  private static String titleCase(String value) {
    return value.charAt(0) + value.substring(1).toLowerCase();
  }
}
