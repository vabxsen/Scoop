# Privacy

Scoop is designed to work entirely on your device.

- **No account.** Scoop does not require sign-in and has no user accounts.
- **No backend.** There is no Scoop server. URL analysis and downloading happen locally via the
  bundled yt-dlp/FFmpeg/aria2c runtime.
- **No analytics, no tracking, no ads.** Scoop does not collect usage data or send telemetry.
- **Network access** is used only to fetch metadata and media from the URL you provide, and (for
  format/version bookkeeping) to talk to the site you're downloading from — nothing else.
- **Cookies**, if you ever configure Scoop to use them for authenticated downloads, are stored
  locally and are never uploaded anywhere by Scoop itself.
- **Downloaded files and history** are stored on your device (app-specific storage, with support
  for user-chosen folders planned) and are never uploaded to a server.

This document will be revised as features that touch user data (e.g. cookie import, SAF folder
access) are actually implemented, to describe exactly what they do.
