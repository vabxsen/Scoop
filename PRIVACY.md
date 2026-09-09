# Privacy

Scoop is designed to work entirely on your device.

- **No Scoop account.** Most downloads do not require sign-in. Optional Instagram sign-in is available for Instagram posts that require an authenticated session.
- **No backend.** There is no Scoop server. URL analysis and downloading happen locally via the
  bundled yt-dlp/FFmpeg/aria2c runtime.
- **No analytics, no tracking, no ads.** Scoop does not collect usage data or send telemetry.
- **Network access** is used only to fetch metadata and media from the URL you provide, and (for
  format/version bookkeeping) to talk to the site you're downloading from — nothing else.
- **Instagram sign-in** opens Instagram's own HTTPS page inside an app-local WebView. Scoop does not read password fields or install a JavaScript bridge. Instagram handles passwords, verification and its own website data collection.
- **Session cookies** are stored in Android WebView's private cookie store on this device, excluded from cloud backup and device transfer. Selected Instagram cookies are passed in memory to the local gallery extractor only for Instagram post links and sent to Instagram to authenticate requests. They are not placed in command-line arguments, preferences, download history or Scoop logs. Disconnect under Settings → Sign in → Instagram to remove the WebView cookies and site storage.
- **Downloaded files and history** are stored on your device — either the default Movies/Scoop and
  Music/Scoop and Pictures/Scoop folders, or a folder you choose yourself in Settings → Storage (via Android's
  Storage Access Framework) — and are never uploaded to a server.

Signing in does not bypass Instagram account permissions or guarantee access to private, deleted, restricted or rate-limited posts.
