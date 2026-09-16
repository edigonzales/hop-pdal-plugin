# hop-pdal-plugin

Point cloud transforms for Apache Hop, based on [PDAL](https://pdal.io) through
[pdal-java-bindings](https://github.com/edigonzales/pdal-java-bindings). The bundled PDAL runtime is
part of the plugin, so **no system PDAL installation is required**.

**[Examples](examples/README.md)** · Point cloud value model:
[hop-pointcloud-type-plugin](https://github.com/edigonzales/hop-pointcloud-type-plugin)

## Transforms

All transforms appear in the **Geospatial** category and share the `sogeo-pointcloud` class loader
group with the Point Cloud Type plugin.

| Transform | Purpose |
|---|---|
| Point Cloud Reader | Describe a LAS/LAZ, COPC, EPT, BPF or text source and create a Point Cloud value. |
| Point Cloud Info | Add point count, bounds, CRS and dimension information without reading points. |
| Point Cloud Filter | Keep points matching an expression. |
| Point Cloud Range | Keep points inside dimension ranges such as `Z[400:900]`. |
| Point Cloud Calculator | Calculate dimensions (`filters.assign`): `Foo = Z * 2`, range assignments, conditions. |
| Point Cloud Classification | Set the ASPRS `Classification` dimension. |
| Point Cloud Thin | Decimation, voxel downsize, grid decimation, Poisson sampling or farthest point sampling. |
| Point Cloud Outlier | Statistical or radius based outlier removal. |
| Point Cloud Sort | Sort by dimensions or Morton code. |
| Point Cloud Transform | Apply a 4x4 transformation matrix. |
| Point Cloud Ground | SMRF/PMF/CSF ground classification plus optional height above ground. |
| Point Cloud Crop | Plan a two or three dimensional bounds crop; remote COPC/EPT crops are pushed into the reader. |
| Point Cloud Reproject | Plan a CRS transformation. |
| Point Cloud Raw Pipeline | Append or replace raw PDAL stage JSON (expert mode). |
| Point Cloud Merger | Merge many unprocessed point clouds into one plan (for example many tiles into one COPC). |
| Point Cloud Statistics | Execute the pipeline and add per dimension statistics (count, min, max, mean, stddev, variance, skewness, kurtosis). |
| Point Cloud to Rows | Execute the pipeline and emit one Hop row per point with block-wise dimension reads. |
| Point Cloud Writer | Execute the planned pipeline **once** and write the result to a file. |

Planning and execution are separated: reader, info, filters, ground classification and the merger
only describe the work; the writer appends the output stage and runs the complete PDAL pipeline in
one native call. A pipeline `Reader → Crop → Ground → Writer` never writes intermediate files.

### Remote COPC/EPT

Remote COPC and EPT sources are read with HTTP range requests. The Point Cloud Crop transform
pushes its bounds into the reader stage, so only the octree nodes that intersect the crop are
requested - an extract of a multi-gigabyte public point cloud transfers a few kilobytes and runs in
seconds instead of downloading the whole file.

## Requirements

- **Java 25** (the bundled PDAL runtime uses the final FFM API) and
  `--enable-native-access=ALL-UNNAMED` in the Hop JVM options (`HOP_OPTIONS`)
- Apache Hop **2.19.0**
- The [Point Cloud Type plugin](https://github.com/edigonzales/hop-pointcloud-type-plugin),
  installed once under `plugins/misc/hop-pointcloud-type` (supplies the shared value model)
- `ch.so.agi:pdal-ffm-core` and the platform `pdal-ffm-natives` classifier JAR, bundled in the
  plugin ZIP (currently resolved from the local Maven repository, see *Building*)

## Installation

```sh
unzip hop-pdal-plugin-0.1.0-SNAPSHOT.zip -d /opt/hop
```

The ZIP installs `plugins/transforms/pdal` with the transform JAR, `pdal-ffm-core` and the native
runtime for the platform it was built for, plus a `dependencies.xml` that points to the Point Cloud
Type plugin. The native runtime makes the ZIP platform specific: build or download the artifact for
your operating system (for example `hop-pdal-plugin-linux-x86_64`).

Add native access to the launcher:

```sh
export HOP_OPTIONS="-Xmx2048m --enable-native-access=ALL-UNNAMED"
```

Without that flag the bundled runtime reports `Native access is not enabled`.

## Usage

```sh
HOP_OPTIONS="-Xmx2048m --enable-native-access=ALL-UNNAMED" \
  hop-run.sh --file examples/02-crop.hpl -r local \
  -p INPUT_FILE=/data/tile.laz \
  -p OUTPUT_FILE=/data/cropped.laz \
  -p MIN_X=2600000 -p MIN_Y=1200000 -p MAX_X=2600500 -p MAX_Y=1200500
```

The writer logs the number of written points:

```
Write point cloud.0 - Wrote 20001 points to /data/cropped.laz
```

Many tiles into one COPC:

```sh
HOP_OPTIONS="-Xmx2048m --enable-native-access=ALL-UNNAMED" \
  hop-run.sh --file examples/11-merge-to-copc.hpl -r local \
  -p INPUT_FILE_1=/data/tile-1.laz -p INPUT_FILE_2=/data/tile-2.laz \
  -p OUTPUT_FILE=/data/merged.copc.laz
```

Merging holds all points in native memory (about 90 bytes per point); use the batch option or an
external builder (Untwine) for very large collections.

## Building

```sh
mvn -B -ntp clean verify                      # unit tests (natives skipped)
PDAL_RUN_INTEGRATION=true mvn -B -ntp verify  # plus the native integration tests
```

The build resolves `ch.so.agi:pdal-ffm-*` and `ch.so.agi:hop-pointcloud-*` from the local Maven
repository. With unpublished snapshots, publish them first:

```sh
cd ../pdal-java-bindings && ./gradlew publishToMavenLocal
cd ../hop-pointcloud-type-plugin && mvn -B -ntp install
```

The natives artifact is selected by a Maven profile per platform
(`pdal-natives-osx-aarch64`, `pdal-natives-linux-x86_64`, ...) and bundled into the plugin ZIP by
the assembly.

## CI

`.github/workflows/ci.yml` builds the complete chain per platform: it stages the PDAL natives from
`pdal-java-bindings`, publishes them to the local Maven repository, verifies remote COPC reads over
TLS, installs the Point Cloud Type plugin, then builds and tests this plugin including the native
integration tests. This keeps CI self-contained while the `pdal-ffm` artifacts are not yet
published to a Maven repository.

## Roadmap

- Merging with per-source filters (DAG plans) and multiple writers
- Publishing `pdal-ffm-*` and the plugins to `jars.interlis.guru` (removes the local build step)
