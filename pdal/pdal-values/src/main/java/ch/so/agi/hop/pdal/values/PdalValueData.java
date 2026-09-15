package ch.so.agi.hop.pdal.values;

import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.pipeline.transform.BaseTransformData;

public final class PdalValueData extends BaseTransformData {
  public IRowMeta inputMeta;
  public IRowMeta outputMeta;
  public final PdalBackend backend = new PdalBackend();
  public boolean emitted;
}
