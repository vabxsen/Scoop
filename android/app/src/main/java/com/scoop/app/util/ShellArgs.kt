package com.scoop.app.util

/** Splits a raw yt-dlp arguments string into tokens, honoring single/double quotes around values
 * (e.g. `--output "%(title)s.%(ext)s"` -> ["--output", "%(title)s.%(ext)s"]). The result is passed
 * straight to YoutubeDLRequest.addCommands() as a token list, not through a shell - so this is
 * purely for splitting user-typed flags apart, not a security boundary. */
fun tokenizeShellArgs(input: String): List<String> {
    val tokens = mutableListOf<String>()
    val current = StringBuilder()
    var quote: Char? = null
    for (c in input) {
        when {
            quote != null ->
                if (c == quote) {
                    quote = null
                } else {
                    current.append(c)
                }
            c == '"' || c == '\'' -> quote = c
            c.isWhitespace() ->
                if (current.isNotEmpty()) {
                    tokens += current.toString()
                    current.clear()
                }
            else -> current.append(c)
        }
    }
    if (current.isNotEmpty()) tokens += current.toString()
    return tokens
}
