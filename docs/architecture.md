# Architecture

## Planning and execution

The transforms separate *planning* from *execution*:

1. **Point Cloud Reader** describes the source with PDAL's preview mode (header based: point count,
   bounds, CRS, dimension layout) and creates a `PointCloudDataset` value whose plan starts with
   the reader stage.
2. **Point Cloud Info** reads the value model only. It never touches native code.
3. **Point Cloud Crop** and **Point Cloud Reproject** append one filter stage each and update the
   planned schema (bounds intersection, unknown point count, target CRS).
4. **Point Cloud Writer** appends the writer stage that matches the output extension and executes
   the complete plan in a single `Pdal.execute` call.

Consequences:

- intermediate point clouds are never materialized and never cross the Java/native boundary
- a chain of N filter transforms costs one native call, not N
- the reader is cheap even for very large files (preview reads headers / a few points)
- error messages carry the source, the consumer transform and the PDAL log excerpt

## Pipeline JSON

`PdalPipelineJson` renders a `PdalPlan` as the PDAL pipeline document. Option values are plain
text; values that are valid JSON booleans, `null` or numbers are embedded unquoted so that numeric
and boolean provider options stay typed. Everything else is rendered as a JSON string, which is
what PDAL expects for paths, CRS codes and bounds expressions.

Reader and writer stage types are inferred from the file extension (`.copc.laz` before `.laz`,
`ept.json`, text and raster formats). Unsupported extensions fail while planning, not while
executing.

The `overwrite` option of the writer is handled in Java (`Files.delete` after an explicit check)
because PDAL's writers do not share a common overwrite option.

## Native runtime and class loading

The plugin depends on `pdal-ffm-core` and the platform `pdal-ffm-natives` classifier JAR. Both are
bundled in the plugin's `lib/` directory, which Hop adds to the plugin class loader. Because the
transforms live in the shared `sogeo-pointcloud` class loader group, `NativeLoader` searches the
thread context class loader, its own class loader and the system class loader for the native
manifest (`META-INF/pdal-native/<classifier>/manifest.json`) and requires exactly one match.

The runtime is extracted into `${java.io.tmpdir}/pdal-ffm/<cacheKey>/<classifier>` on first use.
Data directories (`GDAL_DATA`, `PROJ_DATA`, `PDAL_DRIVER_PATH`) and the bundled CA bundle are
exported through the native shim before the first pipeline runs.

Java 25 is mandatory (final FFM API since Java 22) and the Hop JVM needs
`--enable-native-access=ALL-UNNAMED`, because plugins are loaded from the unnamed module.

## Packaging

```
plugins/transforms/pdal/
  hop-pdal-values-<version>.jar
  dependencies.xml                 -> ../../misc/hop-pointcloud-type[/lib]
  lib/pdal-ffm-core-<version>.jar
  lib/pdal-ffm-natives-<version>-natives-<classifier>.jar
```

The shared point cloud model (`hop-pointcloud-core`/`hop-pointcloud-type`) is deliberately **not**
bundled: it is installed once by the Point Cloud Type plugin so that `PointCloudDataset` keeps a
single class identity across all plugin class loaders. The native runtime makes the ZIP platform
specific; the Maven profiles select the matching classifier.

## Limits (V1)

- the reader infers the PDAL reader from the extension; explicit stage types follow with the filter
  transforms
- bounds and point count are planning values; exact statistics require execution
- reprojection updates the CRS but keeps the source bounds until the writer runs
- grid based datum shifts need optional PROJ grid data that is not part of the standard bundle
- one writer per pipeline is recommended; optional PDAL plugins (E57, TileDB, ...) are not bundled
