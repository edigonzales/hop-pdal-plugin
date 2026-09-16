package ch.so.agi.hop.pdal.values;

import ch.so.agi.hop.pointcloud.PdalPlan;
import ch.so.agi.hop.pointcloud.PointDataType;
import ch.so.agi.pdal.ffm.Pdal;
import ch.so.agi.pdal.ffm.PdalPreview;
import ch.so.agi.pdal.ffm.PdalView;
import java.util.ArrayList;
import java.util.List;

/**
 * Block-wise reader over the point views of an executed plan.
 *
 * <p>Executes the plan through {@link Pdal#open(String)} (standard mode, points stay in native
 * memory) and copies blocks of the selected dimensions into Java arrays. Used by the point-to-rows
 * transform; close it to release the native memory.
 */
final class PdalBlockReader implements AutoCloseable {
  static final int DEFAULT_BLOCK_SIZE = 65536;

  private final PdalView view;
  private final List<String> dimensions;
  private final int blockSize;
  private final List<PointDataType> types = new ArrayList<>();
  private final List<Boolean> floating = new ArrayList<>();
  private double[][] doubleBlocks;
  private long[][] longBlocks;

  private int viewIndex = -1;
  private long offsetInView;
  private int blockLength;

  private PdalBlockReader(PdalView view, List<String> dimensions, int blockSize) {
    this.view = view;
    this.dimensions = dimensions;
    this.blockSize = blockSize;
    this.doubleBlocks = new double[dimensions.size()][];
    this.longBlocks = new long[dimensions.size()][];
    resolveViewDimensions(0);
  }

  static PdalBlockReader open(PdalPlan plan, List<String> dimensions, int blockSize) {
    if (dimensions.isEmpty()) {
      throw new IllegalArgumentException("At least one dimension is required");
    }
    PdalView view = Pdal.open(PdalPipelineJson.render(plan));
    try {
      var reader = new PdalBlockReader(view, List.copyOf(dimensions), Math.max(1, blockSize));
      reader.validateDimensions();
      return reader;
    } catch (RuntimeException e) {
      view.close();
      throw e;
    }
  }

  private void validateDimensions() {
    for (int viewIndex = 0; viewIndex < view.viewCount(); viewIndex++) {
      List<String> available =
          view.dimensions(viewIndex).stream().map(PdalPreview.PdalDimension::name).toList();
      for (String dimension : dimensions) {
        if (!available.contains(dimension)) {
          throw new IllegalArgumentException(
              "Dimension '"
                  + dimension
                  + "' is missing in point view "
                  + viewIndex
                  + " (available: "
                  + available
                  + ")");
        }
      }
    }
  }

  /** Resolves the dimension types of the given view (types may differ between views). */
  private void resolveViewDimensions(int viewIndex) {
    types.clear();
    floating.clear();
    List<PdalPreview.PdalDimension> available = view.dimensions(viewIndex);
    for (String dimension : dimensions) {
      String type = "UNKNOWN";
      for (PdalPreview.PdalDimension candidate : available) {
        if (candidate.name().equals(dimension)) {
          type = candidate.type();
          break;
        }
      }
      PointDataType dataType;
      try {
        dataType = PointDataType.valueOf(type);
      } catch (IllegalArgumentException e) {
        dataType = PointDataType.UNKNOWN;
      }
      types.add(dataType);
      floating.add(dataType.isFloatingPoint() || dataType == PointDataType.UNKNOWN);
    }
  }

  long pointCount() {
    return view.pointCount();
  }

  int blockLength() {
    return blockLength;
  }

  PointDataType type(int dimension) {
    return types.get(dimension);
  }

  /**
   * Reads the next block of all selected dimensions.
   *
   * @return {@code false} when all views are exhausted
   */
  boolean readBlock() {
    while (true) {
      if (viewIndex < 0 || offsetInView >= view.pointCount(viewIndex)) {
        viewIndex++;
        offsetInView = 0;
        if (viewIndex >= view.viewCount()) {
          return false;
        }
        resolveViewDimensions(viewIndex);
        continue;
      }

      int count = (int) Math.min(blockSize, view.pointCount(viewIndex) - offsetInView);
      for (int dimension = 0; dimension < dimensions.size(); dimension++) {
        if (floating.get(dimension)) {
          doubleBlocks[dimension] =
              view.readDoubles(viewIndex, dimensions.get(dimension), offsetInView, count);
        } else {
          longBlocks[dimension] =
              view.readInts(viewIndex, dimensions.get(dimension), offsetInView, count);
        }
      }
      blockLength = count;
      offsetInView += count;
      return true;
    }
  }

  /** Value of a dimension inside the current block as {@code Double} or {@code Long}. */
  Object value(int dimension, int index) {
    if (index < 0 || index >= blockLength) {
      throw new IllegalArgumentException("Point index out of block: " + index);
    }
    if (floating.get(dimension)) {
      return doubleBlocks[dimension][index];
    }
    return longBlocks[dimension][index];
  }

  @Override
  public void close() {
    view.close();
  }
}
