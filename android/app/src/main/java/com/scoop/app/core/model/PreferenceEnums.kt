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
