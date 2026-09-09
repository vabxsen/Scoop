package com.scoop.app.extractor

import org.junit.Assert.*
import org.junit.Test

class ImageParsingTest {
    @Test fun signaturesRejectHtmlEvenWhenUrlOrHeaderClaimsImage() {
        assertNull(ImageFormats.detect("<!doctype html><html>Log in</html>".toByteArray()))
        assertNull(ImageFormats.detect(byteArrayOf(1, 2)))
        assertEquals("image/png", ImageFormats.detect(byteArrayOf(0x89.toByte(), 80, 78, 71, 13, 10, 26, 10)))
        assertEquals("image/jpeg", ImageFormats.detect(byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte())))
        assertEquals("image/svg+xml", ImageFormats.detect("<?xml version=\"1.0\"?><svg xmlns=\"http://www.w3.org/2000/svg\"></svg>".toByteArray()))
    }

    @Test fun pageImagesResolveRelativeUrlsDeduplicateAndPreferLargestSource() {
        val result = WebImageParser.parse("""
            <title>Gallery</title>
            <meta property="og:image" content="/hero.jpg">
            <img src="/hero.jpg"><img alt="Landscape" src="small.jpg" srcset="small.jpg 320w, large.jpg 1280w">
            <img data-src="lazy.webp"><img src="data:image/png;base64,AAAA">
            <a href="../original.png">Original</a><img src="javascript:alert(1)">
        """.trimIndent(), "https://example.org/photos/page")
        assertEquals("Gallery", result.title)
        assertEquals(listOf("https://example.org/hero.jpg", "https://example.org/photos/large.jpg", "https://example.org/photos/lazy.webp", "https://example.org/original.png"), result.images.map { it.url })
        assertEquals("Landscape", result.images[1].title)
        assertTrue(result.images.all { it.headers["Referer"] == "https://example.org/photos/page" })
    }

    @Test fun pageDiscoveryIsBounded() {
        val result = WebImageParser.parse((1..500).joinToString("") { "<img src='/image-$it.png'>" }, "https://example.org")
        assertEquals(200, result.images.size)
        assertTrue(result.notice!!.contains("200"))
    }
}
