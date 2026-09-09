# Image feature: third-party software

Scoop's Kotlin code remains under its existing GPL-3.0 license. These independently
licensed dependencies retain their original licenses and copyright notices.

| Software | Version | License | Upstream |
|---|---|---|---|
| gallery-dl | 1.32.11 | GPL-2.0-only | https://codeberg.org/mikf/gallery-dl |
| Requests | 2.34.2 | Apache-2.0 | https://github.com/psf/requests |
| urllib3 | 2.7.0 | MIT | https://github.com/urllib3/urllib3 |
| charset-normalizer | 3.5.1 | MIT | https://github.com/jawah/charset_normalizer |
| idna | 3.19 | BSD-3-Clause | https://github.com/kjd/idna |
| certifi | 2026.7.22 | MPL-2.0 | https://github.com/certifi/python-certifi |
| jsoup | 1.23.2 | MIT | https://jsoup.org/ |
| Coil SVG | 2.7.0 | Apache-2.0 | https://github.com/coil-kt/coil |
| desugar_jdk_libs | 2.1.5 | GPL-2.0 with Classpath Exception | https://github.com/google/desugar_jdk_libs |

gallery-dl and its Python dependencies run as a separate process. Their Python
source and distributed notices are included inside the shipped runtime archive,
including each package's `.dist-info` license files. The exact source wheel URLs
and hashes are recorded in `android/scripts/gallery-packages.json`, and the
archive-building script is included alongside that manifest.

The Android Python executable and native libraries continue to come from Scoop's
existing youtubedl-android dependency; image support adds no second interpreter.
