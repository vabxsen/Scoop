package com.scoop.app.core.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/** A selectable accent palette. Background/surface always stay the ink/cream base; this only
 * swaps the primary/secondary/tertiary roles used for buttons, selection, and chips. */
enum class AccentPalette {
    MONOCHROME,
    OCEAN,
    FOREST,
    SUNSET,
    LAVENDER,
}

/** A default video quality preference. [heightPx] is null for BEST (no target height to match against). */
enum class DefaultVideoQuality(val label: String, val heightPx: Int?) {
    BEST("Best available", null),
    Q1080("1080p", 1080),
    Q720("720p", 720),
    Q480("480p", 480),
    ASK_EACH_TIME("Ask every time", null),
}

enum class DefaultAudioFormat(val label: String, val container: String) {
    MP3("MP3", "mp3"),
    M4A("M4A", "m4a"),
    OPUS("Opus", "opus"),
}

/** The container video downloads get merged/remuxed into. [ytDlpValue] feeds yt-dlp's
 * --merge-output-format flag directly. */
enum class DefaultVideoContainer(val label: String, val ytDlpValue: String) {
    MP4("MP4", "mp4"),
    MKV("MKV", "mkv"),
    WEBM("WebM", "webm"),
}

/** Audio encode quality for extracted audio. [ytDlpValue] feeds yt-dlp's --audio-quality flag
 * (0 = best VBR, 9 = smallest/lowest). */
enum class AudioQuality(val label: String, val ytDlpValue: String) {
    BEST("Best", "0"),
    STANDARD("Standard", "5"),
    SMALL("Smallest file", "9"),
}

/** How many times a failed download auto-retries (with a short backoff) before it's marked
 * Failed for good. A manual retry from the Downloads screen always gets a fresh budget. */
enum class AutoRetryPolicy(val label: String, val maxAttempts: Int) {
    OFF("Off", 0),
    ONCE("Once", 1),
    THREE_TIMES("Up to 3 times", 3),
    FIVE_TIMES("Up to 5 times", 5),
}

/** Caps download bandwidth. [ytDlpValue] feeds yt-dlp's --limit-rate flag directly; null means unlimited. */
enum class DownloadSpeedLimit(val label: String, val ytDlpValue: String?) {
    UNLIMITED("Unlimited", null),
    KBPS_500("500 KB/s", "500K"),
    MBPS_1("1 MB/s", "1M"),
    MBPS_2("2 MB/s", "2M"),
    MBPS_5("5 MB/s", "5M"),
}

/** Pauses new downloads below this battery level (ignored while charging). Null means disabled. */
enum class BatteryPauseThreshold(val label: String, val percent: Int?) {
    OFF("Off", null),
    BELOW_10("Below 10%", 10),
    BELOW_15("Below 15%", 15),
    BELOW_20("Below 20%", 20),
    BELOW_25("Below 25%", 25),
}

/** Auto-deletes completed downloads (history entry + file) older than this many days. Null means never. */
enum class HistoryRetention(val label: String, val days: Int?) {
    OFF("Off", null),
    DAYS_7("After 7 days", 7),
    DAYS_14("After 14 days", 14),
    DAYS_30("After 30 days", 30),
    DAYS_60("After 60 days", 60),
}
