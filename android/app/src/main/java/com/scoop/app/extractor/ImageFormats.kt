package com.scoop.app.extractor

/** File signatures take precedence over server headers and URL suffixes. */
object ImageFormats {
    private val extensions = mapOf(
        "image/jpeg" to "jpg", "image/png" to "png", "image/gif" to "gif",
        "image/webp" to "webp", "image/avif" to "avif", "image/heic" to "heic",
        "image/heif" to "heif", "image/bmp" to "bmp", "image/tiff" to "tiff",
        "image/svg+xml" to "svg", "image/jxl" to "jxl", "image/x-icon" to "ico",
    )

    fun extension(mimeType: String): String? = extensions[mimeType.substringBefore(';').lowercase()]

    fun detect(bytes: ByteArray): String? {
        fun starts(vararg signature: Int) = bytes.size >= signature.size && signature.indices.all { (bytes[it].toInt() and 255) == signature[it] }
        val ascii = bytes.toString(Charsets.ISO_8859_1)
        return when {
            starts(0xff, 0xd8, 0xff) -> "image/jpeg"
            starts(0x89, 0x50, 0x4e, 0x47, 13, 10, 26, 10) -> "image/png"
            ascii.startsWith("GIF87a") || ascii.startsWith("GIF89a") -> "image/gif"
            ascii.startsWith("RIFF") && ascii.length >= 12 && ascii.substring(8, 12) == "WEBP" -> "image/webp"
            ascii.length >= 16 && ascii.substring(4, 8) == "ftyp" -> when {
                "avif" in ascii.take(64) || "avis" in ascii.take(64) -> "image/avif"
                "heic" in ascii.take(64) || "heix" in ascii.take(64) -> "image/heic"
                "mif1" in ascii.take(64) || "msf1" in ascii.take(64) -> "image/heif"
                else -> null
            }
            starts(0x42, 0x4d) -> "image/bmp"
            starts(0x49, 0x49, 42, 0) || starts(0x4d, 0x4d, 0, 42) -> "image/tiff"
            starts(0xff, 0x0a) || starts(0, 0, 0, 12, 0x4a, 0x58, 0x4c, 0x20) -> "image/jxl"
            starts(0, 0, 1, 0) -> "image/x-icon"
            Regex("^(?:\\uFEFF)?\\s*(?:<\\?xml[^>]*>\\s*)?(?:<!--.*?-->\\s*)*<svg(?:\\s|>)", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                .containsMatchIn(bytes.toString(Charsets.UTF_8)) -> "image/svg+xml"
            else -> null
        }
    }
}
