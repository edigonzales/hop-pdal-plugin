# Examples

Executable Hop pipelines for the point cloud transforms. Every transform ID appears in at least one
example so the documentation lint can verify that the transforms are usable.

| Pipeline | Transforms |
| --- | --- |
| `01-read-write.hpl` | Point Cloud Reader → Point Cloud Writer |
| `02-crop.hpl` | Point Cloud Reader → Point Cloud Crop → Point Cloud Writer |
| `03-reproject.hpl` | Point Cloud Reader → Point Cloud Reproject → Point Cloud Writer |
| `04-info.hpl` | Point Cloud Reader → Point Cloud Info |
| `05-filter-calculator.hpl` | Point Cloud Reader → Point Cloud Filter → Point Cloud Calculator → Writer |
| `06-range-classification.hpl` | Point Cloud Reader → Point Cloud Range → Point Cloud Classification → Writer |
| `07-thin-outlier.hpl` | Point Cloud Reader → Point Cloud Outlier → Point Cloud Thin → Writer |
| `08-sort-transform.hpl` | Point Cloud Reader → Point Cloud Transform → Point Cloud Sort → Writer |
| `09-ground-hag.hpl` | Point Cloud Reader → Point Cloud Ground (SMRF + HAG) → Writer |
| `10-raw-pipeline.hpl` | Point Cloud Reader → Point Cloud Raw Pipeline → Writer |
| `11-merge-to-copc.hpl` | two Point Cloud Readers → Point Cloud Merger → Writer (COPC) |
| `12-statistics.hpl` | Point Cloud Reader → Point Cloud Statistics |
| `13-point-cloud-to-rows.hpl` | Point Cloud Reader → Point Cloud to Rows |

The pipelines expect a LAS/LAZ (or COPC/EPT/BPF/text) file. Pipelines that end in a Point Cloud
Writer run the whole planned PDAL pipeline once; the reader only reads the header for the schema.
The Statistics and Point Cloud to Rows pipelines execute the plan themselves.

Run an example with the Apache Hop client:

```sh
HOP_OPTIONS="--enable-native-access=ALL-UNNAMED" \
  /opt/hop/hop-run.sh --file examples/02-crop.hpl -r local \
  -p INPUT_FILE=/data/tile.laz -p OUTPUT_FILE=/data/cropped.laz
```
