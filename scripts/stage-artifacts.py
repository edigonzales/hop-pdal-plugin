from pathlib import Path
import shutil

root = Path(__file__).resolve().parents[1]
out = root / "target/publication"
out.mkdir(parents=True, exist_ok=True)

# The parent POM is required to resolve the module POM.
shutil.copy2(root / "pom.xml", out / "hop-pdal-plugin-parent.pom")
module = root / "pdal/pdal-values"
shutil.copy2(module / "pom.xml", out / "hop-pdal-values.pom")
for jar in (module / "target").glob("hop-pdal-values-*.jar"):
    if not jar.name.endswith(("-sources.jar", "-javadoc.jar")):
        shutil.copy2(jar, out / jar.name)
