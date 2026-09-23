# Bundled yt-dlp

`android/app/src/main/res/raw/ytdlp` is the official platform-independent zipapp from:
https://github.com/yt-dlp/yt-dlp/releases/download/2026.08.19/yt-dlp

SHA-256: `1fa6733c37ea6fb51c99ad8fe785e7b7e5f3246c9b980230329d4fb72ed8d4d6`

Verified against the release's SHA2-256SUMS. Includes yt-dlp-ejs 0.8.0.
This resource overrides the older copy in youtubedl-android 0.18.1; that
dependency supplies Python, QuickJS and the Android process wrapper.
`BundledYtDlp` atomically refreshes installed copies on app upgrade and verifies
the packaged checksum. No runtime network updates or remote JS components are enabled.

When updating, change the resource and the version/hash in `BundledYtDlp`
together, and test both a fresh installation and an upgrade. Preserve upstream
license notices in the zipapp (yt-dlp Unlicense; bundled third-party components
carry their own notices).
