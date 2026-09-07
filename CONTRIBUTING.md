# Contributing to Scoop

Thanks for considering contributing. Scoop is a small, single-maintainer project, so the process
is kept lightweight — no CLA, no mandatory issue-before-PR rule for small fixes.

## Getting set up

1. Fork the repository and clone your fork:
   ```bash
   git clone https://github.com/<your-username>/Scoop.git
   cd Scoop
   ```
2. Create a branch for your change:
   ```bash
   git checkout -b fix/short-description
   ```
3. Requirements: JDK 21, Android SDK with `compileSdk 35` installed, and an `ANDROID_HOME`
   environment variable or `local.properties` pointing at your SDK.
4. Build a debug APK:
   ```bash
   cd android
   ./gradlew :app:assembleDebug
   ```
   The APK lands in `android/app/build/outputs/apk/debug/`.

## Making changes

- Follow the existing code style (Kotlin, Jetpack Compose, Material 3 conventions already used
  throughout `android/app/src/main/java/com/scoop/app`). Match the patterns in the file you're
  editing rather than introducing a new one.
- Keep PRs focused — one fix or feature per PR is easier to review than a bundle of unrelated
  changes.
- If you're touching the download/extraction path, be aware that **release builds behave
  differently from debug builds**: R8 minification has previously broken yt-dlp/ffmpeg/aria2c's
  reflection-based internals in ways that only show up in a release build, never in debug. If
  your change affects `extractor/`, `downloader/`, or the `proguard-rules.pro` file, test with
  `./gradlew :app:assembleRelease` and smoke-test the actual release APK's core flow (paste a
  link → analyze → download) before opening the PR, not just the debug build.

## Testing

There's no full CI pipeline yet, so manual verification matters:
- Run the app (debug build is fine for most changes) and exercise the flow your change affects.
- For anything touching downloads, extraction, or storage, actually complete a download and
  confirm the file lands where expected (Downloads history, Movies/Music folder, or your
  configured save folder).
- Existing instrumented/unit tests live under `android/app/src/androidTest` — run them if your
  change is in a covered area.

## Submitting a pull request

- Push your branch and open a PR against `main`.
- Describe what changed and why, and how you tested it.
- Keep the PR description honest about what you did and didn't test — that's more useful than a
  long description.

## Reporting bugs / requesting features

Please use the issue templates rather than a blank issue:
- [Bug report](../../issues/new?template=bug_report.yml)
- [Feature request](../../issues/new?template=feature_request.yml)
