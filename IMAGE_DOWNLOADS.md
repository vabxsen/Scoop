# Image downloads

Scoop supports direct HTTP(S) image links, public image galleries supported by the
bundled gallery-dl extractors, and images declared in ordinary HTML pages.

## Use

- Paste or share a link. Direct images are detected in either mode.
- Choose **Images** when downloading photos from a social post or a webpage that
  also contains video. **Video & audio** keeps the existing media flow and falls
  back to image discovery when media extraction fails.
- Inspect the preview, select individual images or select all, and download.
- Images keep their original bytes and format. Completed images appear in the
  existing queue/history with an Image label and can be opened, shared or deleted.
- Android 10+ saves to **Pictures/Scoop** through MediaStore. A custom save folder
  takes precedence. Older Android versions retain Scoop's app-specific fallback.
- Single-image downloads respect **Configure before download**. Multiple-image
  collections always ask for a selection. Incognito suppresses persistent history.

## Image settings

Open **Settings > Images**, directly below Downloads. **Select all gallery images**
controls whether multi-image results start selected; the preference is saved across
app restarts. Single images remain selected. This page also explains original-quality
saving and links to the existing Storage settings to manage the shared save folder.

For Instagram login-required errors, tap **Sign in to Instagram**, finish signing in
and any verification on Instagram's page, then tap **Use this session**. Scoop retries
the pending image link when you return. The session can be removed with **Disconnect**.
Signing in may still leave a post unavailable if Instagram restricts your account or the post.

## Boundaries

This is broad image support, not a promise that every website works. Instagram has an optional sign-in page under Settings → Sign in → Instagram. Other
sites have no login/cookie-import UI. Private posts your account cannot access, expired URLs, anti-bot pages and sites that
only populate images through JavaScript may be inaccessible. The HTML fallback
can include page icons and thumbnails; it does not claim they are originals.
Extraction is capped at 200 images per link, HTML at 2 MiB, and each downloaded
image at 256 MiB. The app shows a notice when discovery reaches the image cap.
Some formats can be saved but cannot be previewed by a particular Android version.

## Implementation

- `ImageDiscovery`: checks image signatures, calls gallery-dl, then tries HTML.
- `WebImageParser`: jsoup handles relative URLs, social preview metadata, lazy
  image attributes, responsive sources and direct image anchors; URLs are deduplicated.
- `GalleryImageExtractor`: invokes a separate Python process using the Python
  runtime in the pinned youtubedl-android 0.17.3 dependency. It does not replace
  yt-dlp, import browser cookies, or load a user's gallery-dl configuration.
- `ImageDownloader`: streams through OkHttp, verifies signatures, enforces size
  and speed limits, closes requests on cancellation, and publishes through the
  existing storage layer. Sensitive headers are stripped across origin redirects.
- `DownloadManagerImpl`: dispatches image requests to that handler and keeps the
  shared concurrency, Wi-Fi/battery admission, retry and history behavior.
- `DownloadService` stops itself only after calling `startForeground`, avoiding
  a startup/stop race exposed by very small image downloads.

No database migration is required: history already stores download kinds as text.
Image request headers remain in memory; persisted image thumbnails use the local
saved URI instead of storing a temporary remote image URL.

## Updating gallery-dl

The APK ships `android/app/src/main/assets/gallery/runtime.zip`, containing pure
Python package source and the upstream license notices. Builds work without pip
or downloading Python packages. To reproduce the archive:

```sh
cd android
python scripts/bundle_gallery.py
```

`scripts/gallery-packages.json` pins exact wheel URLs and SHA-256 hashes. Review
and update those pins intentionally, rebuild the archive, bump the runtime directory
revision in `GalleryImageExtractor`, and run the device tests. See
`THIRD_PARTY_IMAGE_NOTICES.md` for sources and licenses.

## Verification

```sh
cd android
./gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest :app:assembleRelease
```

Unit tests cover image signatures and HTML discovery. Device tests run the bundled
gallery runtime, download extensionless/redirected images, compare original bytes,
check MediaStore output, reject HTML masquerading as an image, cancel partial
downloads, and exercise rapid queue completion with ordinary/incognito history.
The connected test runner may uninstall the debug app afterward; installDebug
restores it for manual UI checks. A release build also needs an ARM64 device smoke
test before publication.
