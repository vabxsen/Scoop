# Privacy

Scoop is designed to work entirely on your device.

- **No Scoop account.** Most downloads do not require sign-in. Optional Instagram sign-in is available for Instagram posts that require an authenticated session.
- **No backend.** There is no Scoop server. URL analysis and downloading happen locally via the
  bundled yt-dlp/FFmpeg/aria2c runtime.
- **No analytics, no tracking, no ads.** Scoop does not collect usage data or send telemetry.
- **Network access** is used to fetch metadata and media from public HTTPS destinations reached
  from the URL you provide, and to check Scoop's GitHub Releases feed when you request an app update.
- **Instagram sign-in** opens Instagram's own HTTPS page inside an app-local WebView. Scoop does not read password fields or install a JavaScript bridge. Instagram handles passwords, verification and its own website data collection.
- **Session cookies** are stored in Android WebView's private cookie store on this device, excluded from cloud backup and device transfer. Selected Instagram cookies are passed in memory to the local gallery extractor only for Instagram post links and sent to Instagram to authenticate requests. They are not placed in command-line arguments, preferences, download history or Scoop logs. Use Disconnect on the Instagram sign-in screen to remove the WebView cookies and site storage.
- **Downloaded files and history** are stored on your device. Android 10+ uses the shared
  Movies/Scoop, Music/Scoop and Pictures/Scoop collections; Android 7–9 uses app-private storage;
  a folder you choose in Settings → Storage overrides either default. Automatic history retention
  removes records only and never deletes media. Explicit Delete/Clear all actions do delete files.
- **Queue recovery** stores pending requests in app-private, no-backup storage. Cookies and
  authorization headers are stripped before that queue is written; authenticated image jobs must
  be analyzed again after a process restart. Incognito jobs are not written to the recovery journal.
- **Executable updates** are accepted only through signed Scoop APK releases. The bundled yt-dlp
  runtime does not self-update independently.

Signing in does not bypass Instagram account permissions or guarantee access to private, deleted, restricted or rate-limited posts.
