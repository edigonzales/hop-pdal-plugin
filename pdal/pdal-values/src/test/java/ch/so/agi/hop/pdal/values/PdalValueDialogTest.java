package ch.so.agi.hop.pdal.values;

import static org.assertj.core.api.Assertions.assertThat;

import ch.so.agi.hop.commons.core.SourceMode;
import ch.so.agi.hop.commons.core.ValueOrField;
import ch.so.agi.hop.commons.ui.ValueOrFieldControl;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.apache.hop.core.HopClientEnvironment;
import org.apache.hop.core.annotations.Transform;
import org.apache.hop.core.plugins.PluginRegistry;
import org.apache.hop.core.plugins.TransformPluginType;
import org.apache.hop.core.variables.Variables;
import org.apache.hop.core.xml.XmlHandler;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PdalValueDialogTest {
  @ParameterizedTest
  @CsvSource({"READER,true,true", "WRITER,true,false", "REPROJECT,true,true",
      "CROP,true,true", "CROP,true,false", "CROP,false,true"})
  void savesTheActiveValuesAndDiscardsCancelledEdits(String operation, boolean accept, boolean byField)
      throws Exception {
    HopClientEnvironment.init();
    PdalValueMeta meta = switch (operation) {
      case "READER" -> new PointCloudReaderMeta();
      case "WRITER" -> new PointCloudWriterMeta();
      case "REPROJECT" -> new PointCloudReprojectMeta();
      default -> new PointCloudCropMeta();
    };
    PluginRegistry.getInstance().registerPluginClass(
        meta.getClass().getName(), TransformPluginType.class, Transform.class);
    meta.setDefault();
    meta.setValueField("points");
    meta.setSource("/tmp/original.laz");
    meta.setTargetCrs("EPSG:2056");
    meta.setMinX("1");
    meta.setMinY("2");
    meta.setMinZ("3");
    meta.setMaxX("4");
    meta.setMaxY("5");
    meta.setMaxZ("6");
    String original = meta.getXml();
    var pipeline = new PipelineMeta();
    pipeline.addTransform(new TransformMeta("Configure point cloud", meta));
    Display display = Display.getDefault();
    Shell parent = new Shell(display);
    var failure = new AtomicReference<Throwable>();
    display.asyncExec(() -> {
      Shell shell = Arrays.stream(parent.getShells()).findFirst().orElse(null);
      try {
        assertThat(shell).isNotNull();
        var editors = editors(shell);
        assertThat(editors).isNotEmpty();
        for (int i = 0; i < editors.size(); i++) {
          var editor = editors.get(i);
          editor.setValue(new ValueOrField(SourceMode.CONFIGURED, "configured_" + i, "field_" + i));
        }
        var selector = (Combo) editors.getLast().getChildren()[0];
        selector.select(1);
        selector.notifyListeners(SWT.Selection, new Event());
        assertThat(editors).allSatisfy(editor -> assertThat(editor.getValue().mode())
            .isEqualTo(SourceMode.FIELD));
        if (!byField) {
          selector.select(0);
          selector.notifyListeners(SWT.Selection, new Event());
        }
        for (int i = 0; i < editors.size(); i++)
          assertThat(editors.get(i).getValue()).isEqualTo(new ValueOrField(
              byField ? SourceMode.FIELD : SourceMode.CONFIGURED,
              "configured_" + i, "field_" + i));
        assertThat(meta.getXml()).isEqualTo(original);
        Arrays.stream(shell.getChildren())
            .filter(Composite.class::isInstance)
            .map(Composite.class::cast)
            .flatMap(panel -> Arrays.stream(panel.getChildren()))
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .filter(button -> button.getText().equals(accept ? "OK" : "Cancel"))
            .findFirst().orElseThrow().notifyListeners(SWT.Selection, new Event());
        assertThat(shell.isDisposed()).isTrue();
      } catch (Throwable error) {
        failure.set(error);
        if (shell == null) parent.dispose();
        else if (!shell.isDisposed()) shell.dispose();
      }
    });
    try {
      String result = new PdalValueDialog(parent, new Variables(), meta, pipeline).open();
      if (failure.get() != null) throw new AssertionError("PDAL dialog interaction", failure.get());
      if (!accept) {
        assertThat(result).isNull();
        assertThat(meta.getXml()).isEqualTo(original);
        return;
      }
      assertThat(result).isEqualTo("Configure point cloud");
      var saved = (PdalValueMeta) meta.clone();
      saved.loadXml(
          XmlHandler.loadXmlString("<transform>" + meta.getXml() + "</transform>").getDocumentElement(),
          null);
      String prefix = byField ? "field_" : "configured_";
      switch (operation) {
        case "READER", "WRITER" -> {
          assertThat(saved.isSourceField()).isEqualTo(byField);
          assertThat(saved.getSource()).isEqualTo(prefix + "0");
        }
        case "REPROJECT" -> {
          assertThat(saved.isTargetCrsField()).isEqualTo(byField);
          assertThat(saved.getTargetCrs()).isEqualTo(prefix + "0");
        }
        default -> {
          assertThat(saved.isBoundsFields()).isEqualTo(byField);
          assertThat(List.of(saved.getMinX(), saved.getMinY(), saved.getMinZ(),
              saved.getMaxX(), saved.getMaxY(), saved.getMaxZ()))
              .containsExactly(prefix + "0", prefix + "1", prefix + "2",
                  prefix + "3", prefix + "4", prefix + "5");
        }
      }
    } finally {
      if (!parent.isDisposed()) parent.dispose();
    }
  }

  private static List<ValueOrFieldControl> editors(Composite parent) {
    return Arrays.stream(parent.getChildren()).flatMap(control -> {
      if (control instanceof ValueOrFieldControl editor) return Stream.of(editor);
      if (control instanceof Composite composite) return editors(composite).stream();
      return Stream.empty();
    }).toList();
  }
}
