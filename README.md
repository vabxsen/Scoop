# Scoop

Scoop is a free, local-first video/audio downloader for Android (iOS planned). It resolves a
pasted or shared link, shows you the available formats, and downloads through a bundled yt-dlp
runtime — no account, no backend, no ads.

Scoop's UX and architecture are inspired by [Seal](https://github.com/JunkFood02/Seal)
(GPLv3), studied as a reference for how a native yt-dlp-based downloader should behave. No Seal
code, branding, or assets are used; Scoop is an independent implementation under its own name,
icon, and package id.

## Status

This repository currently contains the **Android foundation**: project scaffold, navigation,
theming, persistence, dependency injection, and a real (not mocked) integration with yt-dlp /
FFmpeg / aria2c — enough to paste a URL, analyze it, and download a video or audio-only file.
Most of the feature surface described for the full app (format picker, playlists, subtitles,
metadata/thumbnail embedding, full settings, download history UI, custom commands, iOS) is not
built yet; see `ARCHITECTURE.md` (added once more of the app exists) for what's implemented.

## Features (so far)

- Paste or share a URL, analyze it, see title/uploader/duration/thumbnail
- Download as video (best available) or audio-only (mp3)
- A live download queue with progress, backed by a foreground service
- Material 3 UI with dynamic color and dark/light/system theme support

## Platforms

- **Android**: Kotlin, Jetpack Compose, Material 3, Koin, Room, MMKV. Min SDK 24, target/compile
  SDK 35.
- **iOS**: not started yet.

## Building

Requirements: JDK 21, Android SDK (compileSdk 35, build-tools 34/35), an `ANDROID_HOME`/
`local.properties` pointing at it.

```
cd android
./gradlew :app:assembleDebug
```

The debug APK lands in `android/app/build/outputs/apk/debug/`.

## Third-party dependencies and licensing

Scoop depends on `io.github.junkfood02.youtubedl-android` (a maintained fork of
[yausername/youtubedl-android](https://github.com/yausername/youtubedl-android)), which bundles:

- [yt-dlp](https://github.com/yt-dlp/yt-dlp) (Unlicense) — the extraction/download engine
- [FFmpeg](https://ffmpeg.org/) (LGPL/GPL depending on build) — media muxing/transcoding
- [aria2](https://aria2.github.io/) (GPLv2) — optional multi-connection download backend

Because Scoop links against this GPL-family native stack, **Scoop itself is licensed under the
GNU General Public License v3.0** — see `LICENSE`. Seal (GPLv3) was used only as an architectural
reference, per the terms described above; no Seal source was copied into this project.

## Privacy

See `PRIVACY.md`. Short version: no account, no analytics, no backend, no data leaves your
device except the requests needed to resolve and download the URL you gave it.
