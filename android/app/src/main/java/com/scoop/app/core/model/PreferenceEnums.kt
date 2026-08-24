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
