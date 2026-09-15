# Examples

Executable Hop pipelines for the point cloud transforms. Every transform ID appears in at least one
example so the documentation lint can verify that the transforms are usable.

| Pipeline | Transforms |
| --- | --- |
| `01-read-write.hpl` | Point Cloud Reader → Point Cloud Writer |
| `02-crop.hpl` | Point Cloud Reader → Point Cloud Crop → Point Cloud Writer |
| `03-reproject.hpl` | Point Cloud Reader → Point Cloud Reproject → Point Cloud Writer |
| `04-info.hpl` | Point Cloud Reader → Point Cloud Info |

The pipelines expect a LAS/LAZ (or COPC/EPT/BPF/text) file. Every pipeline runs the whole planned
PDAL pipeline once in the Point Cloud Writer; the reader only reads the header for the schema.

Run an example with the Apache Hop client:

```sh
HOP_OPTIONS="--enable-native-access=ALL-UNNAMED" \
  /opt/hop/hop-run.sh --file examples/02-crop.hpl -r local \
  -p INPUT_FILE=/data/tile.laz -p OUTPUT_FILE=/data/cropped.laz
```
