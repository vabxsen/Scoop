"""Build the vendored, pure-Python gallery runtime from hash-pinned PyPI wheels.

Run: python scripts/bundle_gallery.py
The Android build uses the checked-in archive and never downloads Python packages.
"""
import hashlib
import json
from pathlib import Path
import urllib.request
import zipfile
import io

ROOT = Path(__file__).resolve().parents[1]

def build():
    packages = json.loads((ROOT / "scripts/gallery-packages.json").read_text())
    target = ROOT / "app/src/main/assets/gallery/runtime.zip"
    target.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(target, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for package in packages:
            data = urllib.request.urlopen(package["url"], timeout=60).read()
            if hashlib.sha256(data).hexdigest() != package["sha256"]:
                raise ValueError("Checksum mismatch: " + package["name"])
            with zipfile.ZipFile(io.BytesIO(data)) as wheel:
                for name in sorted(wheel.namelist()):
                    if name.endswith("/"):
                        continue
                    entry = zipfile.ZipInfo(name, (2026, 1, 1, 0, 0, 0))
                    entry.compress_type = zipfile.ZIP_DEFLATED
                    archive.writestr(entry, wheel.read(name))
    print(target, hashlib.sha256(target.read_bytes()).hexdigest())

if __name__ == "__main__":
    build()
