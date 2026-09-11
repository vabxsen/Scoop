# F-Droid compatibility audit

Audit date: 2026-09-11

Application ID: `com.scoop.app`

Audited version: `1.2.7` (`versionCode` 28)

## Result

Scoop's own code and declared Kotlin/Android dependencies are under free-software-compatible
licenses. The current release is **not ready for the main F-Droid repository** because it packages
prebuilt Python, FFmpeg and aria2c native artifacts and can update executable components outside
the app-store review flow.

The Fastlane metadata in this repository is ready for an eventual F-Droid submission. A submission
should wait until the blockers below have been addressed and the resulting build has been tested.

## Direct dependency review

| Component | Version | License family | F-Droid assessment |
|---|---:|---|---|
| AndroidX / Jetpack Compose / Room | BOM 2025.03.01 / Room 2.6.1 | Apache-2.0 | Compatible |
| Kotlin / coroutines / serialization | 2.0.20 / 1.9.0 / 1.7.2 | Apache-2.0 | Compatible |
| OkHttp / Okio | 5.0.0-alpha.10 / 3.9.0 resolved | Apache-2.0 | Compatible |
| jsoup | 1.23.2 | MIT | Compatible |
| Coil / Coil SVG / AndroidSVG | 2.7.0 / 1.4 | Apache-2.0 | Compatible |
| Koin | 4.0.0 | Apache-2.0 | Compatible |
| MMKV | 1.3.12 | BSD-3-Clause | Compatible, but includes native code |
| youtubedl-android library | 0.17.3 | GPL-3.0 | License-compatible; bundled binaries need source-build handling |
| youtubedl-android FFmpeg | 0.17.3 | GPL/LGPL stack | License-compatible; prebuilt AAR is a blocker |
| youtubedl-android aria2c | 0.17.3 | GPL-2.0 stack | License-compatible; prebuilt AAR is a blocker |
| gallery-dl | 1.32.11 | GPL-2.0-only | Runs as a separate process; source and notices are bundled |
| Requests and gallery Python dependencies | See `android/scripts/gallery-packages.json` | Apache/MIT/BSD/MPL | Compatible; vendored archive needs reproducible source handling |
| desugar_jdk_libs | 2.1.5 | GPL-2.0 with Classpath Exception | Compatible |

The resolved dependency graph also contains Apache-2.0 libraries such as Commons IO, Commons
Compress and Jackson through youtubedl-android. No Firebase, Google Play Services, advertising or
analytics SDK was found in the release runtime graph.

## Blocking work

1. **Build native media tools from source.** The `library`, `ffmpeg` and `aria2c` Maven artifacts
   contain prebuilt Python/native payloads. F-Droid builds should reproduce these from tagged source
   or use separately accepted reproducible binaries with pinned signing/build metadata.
2. **Regenerate the gallery runtime during the F-Droid build.** The checked-in
   `assets/gallery/runtime.zip` is reproducibly generated from hash-pinned wheels, but F-Droid's
   scanner can still treat the archive as a binary blob. Package source through an approved
   `srclib`/prebuild process and recreate the archive in the build recipe.
3. **Disable executable self-updates in the F-Droid flavor.** `MediaEngineReadiness` currently asks
   youtubedl-android to update yt-dlp at startup. F-Droid must receive yt-dlp updates through a new
   reviewed application build.
4. **Remove the APK self-updater from the F-Droid flavor.** That flavor should omit
   `REQUEST_INSTALL_PACKAGES`, its update UI and APK-download code. F-Droid will provide updates.
5. **Verify the isolated build.** Run `fdroid lint`, build the proposed metadata in F-Droid's
   container, compare source and output, then execute Scoop's unit and connected-device tests on
   the F-Droid artifact.

## Distribution notes

The standard arm64 release is about 41 MB. Its compressed size is dominated by FFmpeg (~17 MB),
Python/yt-dlp (~11.5 MB) and aria2c (~4.7 MB). IzzyOnDroid normally caps one APK at 30 MB. Removing
enough data to meet that threshold would remove media capabilities; a safe reduction requires
custom source-built, feature-scoped native packages. Scoop should keep its full feature set and
target main F-Droid first, or request an IzzyOnDroid size exception after reproducible builds are in
place.

Additional ABI builds can be produced independently without making one universal APK larger. See
`android/scripts/build_release_variants.ps1`.
