package com.scoop.app.util

/** Splits a raw yt-dlp arguments string into tokens, honoring single/double quotes around values
 * (e.g. `--output "%(title)s.%(ext)s"` -> ["--output", "%(title)s.%(ext)s"]). The result is passed
 * straight to YoutubeDLRequest.addCommands() as a token list, not through a shell - so this is
 * purely for splitting user-typed flags apart, not a security boundary. */
fun tokenizeShellArgs(input: String): List<String> {
    val tokens = mutableListOf<String>()
    val current = StringBuilder()
    var quote: Char? = null
    // Tracks a token that has started but may still be empty, so `--referer ""` keeps its
    // empty value instead of silently dropping it and shifting the following arguments.
    var inToken = false
    for (c in input) {
        when {
            quote != null ->
                if (c == quote) {
                    quote = null
                } else {
                    current.append(c)
                }
            c == '"' || c == '\'' -> {
                quote = c
                inToken = true
            }
            c.isWhitespace() ->
                if (inToken) {
                    tokens += current.toString()
                    current.clear()
                    inToken = false
                }
            else -> {
                current.append(c)
                inToken = true
            }
        }
    }
    if (inToken) tokens += current.toString()
    return tokens
}
