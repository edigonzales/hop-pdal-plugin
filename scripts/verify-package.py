#!/usr/bin/env python3
from pathlib import Path
from zipfile import ZipFile
from io import BytesIO

root = Path(__file__).resolve().parents[1]
files = list((root / "assemblies/assemblies-hop-pdal/target").glob("hop-pdal-plugin-*.zip"))
assert len(files) == 1, files
with ZipFile(files[0]) as z:
    names = z.namelist()
    jars = [n for n in names if n.endswith(".jar")]
    assert any(n.startswith("plugins/transforms/pdal/hop-pdal-values-") for n in jars), jars
    assert any(
        n.startswith("plugins/transforms/pdal/lib/pdal-ffm-core-") for n in jars
    ), jars
    natives = [n for n in jars if "pdal-ffm-natives" in n and "natives-" in n]
    assert len(natives) == 1, f"expected exactly one bundled native artifact, found {natives}"
    assert "plugins/transforms/pdal/dependencies.xml" in names, "dependencies.xml is missing"
    assert not any("hop-pointcloud-core" in n for n in jars), "shared model must not be bundled"
    assert not any("hop-pointcloud-type" in n for n in jars), "shared type must not be bundled"
    assert not any(n.startswith("plugins/transforms/pdal/lib/org/apache/hop/") for n in names)
    for n in jars:
        with ZipFile(BytesIO(z.read(n))) as jar:
            assert not any(
                c.startswith(("ch/so/agi/hop/pointcloud/", "org/apache/hop/")) for c in jar.namelist()
            ), n
print("PDAL plugin package OK: transforms plus PDAL runtime, shared model referenced")
