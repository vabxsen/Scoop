<div align="center">
  <img src="android/app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.png" width="240" height="240" alt="Scoop logo" />

  # Scoop

  **A free, local-first video & audio downloader for Android.**

  No account. No backend. No ads. Just paste a link and download.

  [![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
  ![Platform](https://img.shields.io/badge/platform-Android-3DDC84)
  ![Min SDK](https://img.shields.io/badge/minSdk-24-informational)

  [Download the latest release](../../releases/latest) · [Report an issue](../../issues)
</div>

---

## What is Scoop?

Scoop resolves a pasted or shared video link, lets you configure exactly how you want it saved,
and downloads it straight to your device through a bundled [yt-dlp](https://github.com/yt-dlp/yt-dlp)
runtime — no server in the middle, no sign-in, nothing leaving your phone beyond the request
needed to fetch the media itself.

## Features

### Downloading
- **Paste or share a link** from any app — Scoop registers as a share target and as a link
  handler, so you can send a URL straight to it or open supported links directly with Scoop.
- **Analyze before downloading**: see the title, uploader, duration, and thumbnail before
  committing to anything.
- **Video or audio-only**, chosen per download.
- **Playlist support**: paste a playlist link and pick exactly which videos to grab from a
  checklist — each one downloads as its own queued item, and your history groups them together
  by playlist title.
- **Live download queue** with real-time progress, running as a foreground service so downloads
  keep going in the background, with a configurable limit on how many run concurrently.

### Quality & format
- **Highest quality / Low quality** — a one-tap choice per download. Highest always resolves to
  the true best format yt-dlp can find for that specific video; Low picks the smallest available.
- Configurable **default video quality, video container, audio format, and audio quality** in
  Settings, for videos you don't want to hand-pick every time.
- **Embed subtitles** — burns in whatever subtitle tracks the source has (manual or
  auto-generated) directly into the video file.
- **Embed thumbnail** — embeds the source's thumbnail as cover art, for both video and audio
  downloads.

### Advanced
- **Custom command**: an optional field for raw `yt-dlp` arguments, applied on top of Scoop's own
  options — for anything the UI doesn't expose yet (SponsorBlock, custom postprocessing, and so
  on).

### Download management
- A full **download history** with filtering (All / Downloading / Completed / Failed), grouped by
  date.
- **Swipe to delete** with an undo window, safe to navigate away from mid-swipe.
- Per-download detail view with retry, cancel, and file actions.
- **Bulk clear** of history with one confirmation.

### Reliability & network behavior
- **Wi-Fi-only downloads** toggle, so nothing eats your mobile data unless you say so.
- **Auto-retry policy** for downloads that fail transiently.
- **Download speed limiting** and a **battery-pause threshold** so background downloads don't
  drain your phone.
- Configurable **history retention**.

### Storage
- Choose your own **save folder** via Android's Storage Access Framework, or use Scoop's default
  Movies/Music folders.
- A **storage usage** view showing space used across every storage volume on your device, broken
  down by video vs. audio.

### Appearance
- **Material 3** UI with **dynamic color** (Android 12+ wallpaper-based theming).
- Five built-in **accent palettes**, for devices without dynamic color or if you just prefer one.
- **Light / Dark / System** theme, applied consistently across the whole app — including the
  status bar.

### Privacy
- No account, no backend, no analytics, no ads.
- Everything runs on-device; see [`PRIVACY.md`](PRIVACY.md) for the full breakdown.

## Installation

Grab the latest signed APK from the [Releases page](../../releases/latest), open it, and allow
"install unknown apps" for your browser or file manager when prompted. Scoop is arm64-v8a only.

## Building from source

Requirements: JDK 21, Android SDK (compileSdk 35), an `ANDROID_HOME`/`local.properties` pointing
at it.

```bash
cd android
./gradlew :app:assembleDebug
```

The debug APK lands in `android/app/build/outputs/apk/debug/`.

## Tech stack

Kotlin, Jetpack Compose, Material 3, Koin (DI), Room (download history), MMKV (preferences).
Min SDK 24, target/compile SDK 35.

## Third-party dependencies and licensing

Scoop depends on `io.github.junkfood02.youtubedl-android` (a maintained fork of
[yausername/youtubedl-android](https://github.com/yausername/youtubedl-android)), which bundles:

- [yt-dlp](https://github.com/yt-dlp/yt-dlp) (Unlicense) — the extraction/download engine
- [FFmpeg](https://ffmpeg.org/) (LGPL/GPL depending on build) — media muxing/transcoding
- [aria2](https://aria2.github.io/) (GPLv2) — optional multi-connection download backend

Because Scoop links against this GPL-family native stack, **Scoop itself is licensed under the
GNU General Public License v3.0** — see [`LICENSE`](LICENSE).

## Credits

Scoop's UX and architecture were designed with [**Seal**](https://github.com/JunkFood02/Seal), by
[**JunkFood02**](https://github.com/JunkFood02), studied closely as a reference for how a
native, on-device yt-dlp downloader should look and behave. No Seal source code, assets, or
branding are used anywhere in this project — Scoop is an independent implementation under its own
name, icon, and package id — but the debt to Seal's design is real, and it's credited here with
thanks. If you're looking for a mature, feature-rich app in this space today, go check out Seal.
