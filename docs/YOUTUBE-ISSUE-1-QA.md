# YouTube thumbnail issue: test candidate

Date: 2026-09-23. Issue: https://github.com/vabxsen/Scoop/issues/1

## Diagnosis and changes

- The bundled yt-dlp was 2024.09.27. Running it with Scoop's forced Android
  player client on `hiMPy5769Xc` reproduced an HTTP 400 / precondition error,
  a missing PO-token warning, and "Only images are available".
- HomeViewModel then silently fell back to webpage image discovery. This could
  even enqueue a thumbnail when configure-before-download was disabled.
- Video/audio failures now stay in the selected mode and display the extraction
  error. Explicit Images mode and direct image URLs remain supported.
- Pinned yt-dlp 2026.08.19 replaces the stale resource; youtubedl-android 0.18.1
  supplies Python 3.12 and QuickJS. Existing FFmpeg 0.17.3 binaries are retained.
  Upstream provenance/checksum is in BUNDLED-YT-DLP.md. The deprecated forced
  Android client is removed from metadata, playlist and download requests.
- Startup atomically installs the pinned runtime, including over old installed
  copies. Invalid packaged resources roll back. Executable self-updates remain
  disabled. Analysis cancellation destroys the child process; timeouts become
  visible errors instead of leaving the sheet loading.

## Verified

- `testDebugUnitTest`: 16 tests passed.
- Nine targeted Android tests passed on Android 15 / x86_64: media failure in
  both configure settings, explicit images, direct images, cancellation,
  runtime replacement/rollback, packaged yt-dlp version, gallery-dl startup,
  redirected image byte preservation, and webpage images / fake-image rejection.
- Fixed missing Android test runner dependency. Debug alone allows HTTP to
  127.0.0.1 for local fixtures; release network policy is unchanged. An old
  fixture assertion now reflects the existing HTTPS-only referrer policy.
- Signed, minified ARM64 and ARMv7 release-mode APKs built, version
  `1.3.1-rc1`, code 32. Correct single ABI and existing signing certificate
  verified for each. Stable defaults remain 1.3.0 / 31.
- `lintRelease`: no errors, 50 warnings. Lint also emitted Kotlin analysis API
  compatibility warnings; this is not a claim that every lint check is complete.
- Existing ARM64 1.3.0 upgraded to the ARM64 candidate without uninstalling and
  opened successfully. ARM subprocess execution could not be tested through the
  emulator's native translation (ARM/x86 linker mismatch).
- A native x86_64 minified release build starts Python and shows YouTube's real
  sign-in error in the video sheet rather than opening the thumbnail picker.
- All three reporter links resolved metadata with the new zipapp on Windows:
  `hiMPy5769Xc`, `Z8OgO5pHwxI`, `8cMzvVg3y10`.

## Not verified / release gate

Live Android YouTube smoke tests did NOT pass. The queue received a sign-in /
bot challenge; metadata also timed out. An isolated visionOS client diagnostic
through the same public-HTTPS proxy returned HTTP 429 and the same challenge.
Further live retries were stopped. This is not evidence of successful video or
audio downloads, nor proof that every user's network will behave the same.

A real ARMv7 device and a real ARM64 device are still needed. Before publishing
a stable release, verify all three links in Video & audio mode, video playback,
audio playback, and an image download. Test upgrading without clearing data.
If a link fails, collect the new on-screen error, Android version and ABI. Do not
ask the reporter to select the mode again as though the earlier report were user
error. Keep issue #1 open until they confirm the result.

## Reproduce

Use a checkout path without spaces (Room/KSP rejects spaces in schema options).
The local `work/scoop-build` directory is a junction to the main checkout, not a
separate copy. Quote Gradle properties in PowerShell.

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest '-PscoopAbi=x86_64'
.\gradlew.bat :app:assembleRelease '-PscoopAbi=armeabi-v7a' '-PscoopVersionName=1.3.1-rc1' '-PscoopVersionCode=32'
.\gradlew.bat :app:assembleRelease '-PscoopAbi=arm64-v8a' '-PscoopVersionName=1.3.1-rc1' '-PscoopVersionCode=32'
```

Live tests are opt-in: instrument `YouTubeSmokeDeviceTest` with
`-e youtubeSmoke true`. The optional client diagnostic runs only when
`-e youtubeClient <client>` is supplied. Do not repeatedly run it after HTTP 429.

APKs are local test candidates, not a verified stable release. No GitHub release,
issue reply or issue closure was performed during this validation.
