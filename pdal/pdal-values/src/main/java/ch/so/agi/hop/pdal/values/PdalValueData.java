package ch.so.agi.hop.pdal.values;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.pipeline.transform.BaseTransformData;

public final class PdalValueData extends BaseTransformData {
  public IRowMeta inputMeta;
  public IRowMeta outputMeta;
  public final PdalBackend backend = new PdalBackend();
  public boolean emitted;

  /** Point-to-rows state: the open block reader and the current input row. */
  public PdalBlockReader blockReader;
  public Object[] pointCloudRow;

  /** Aggregation state of the merger transform, keyed by the group value. */
  public final Map<String, MergedGroup> mergedGroups = new LinkedHashMap<>();

  public static final class MergedGroup {
    public final Object key;
    public final List<ch.so.agi.hop.pointcloud.PointCloudDataset> datasets = new ArrayList<>();
    public int batchesEmitted;

    public MergedGroup(Object key) {
      this.key = key;
    }
  }
}
