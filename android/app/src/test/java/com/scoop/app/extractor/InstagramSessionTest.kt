package com.scoop.app.extractor

import org.junit.Assert.*
import org.junit.Test

class InstagramSessionTest {
    @Test fun onlySecureInstagramOriginsAreAllowed() {
        assertTrue(InstagramSession.isInstagramPage("https://www.instagram.com/accounts/login/"))
        assertTrue(InstagramSession.isInstagramPage("https://help.instagram.com/"))
        listOf("http://instagram.com/p/abc/", "https://instagram.com.evil.test/p/abc/",
            "https://evilinstagram.com/p/abc/", "https://instagram.com@evil.test/p/abc/",
            "https://user@instagram.com/p/abc/", "https://instagram.com:444/p/abc/", "file:///data/data/app")
            .forEach { assertFalse(it, InstagramSession.isInstagramPage(it)) }
    }

    @Test fun postAndShareLinksAreRecognizedWithoutTreatingLoginAsMedia() {
        assertTrue(InstagramSession.isPost("https://www.instagram.com/p/DaPanNNEmMw/?stkn=tracking"))
        assertTrue(InstagramSession.isPost("https://www.instagram.com/share/p/abc/"))
        assertTrue(InstagramSession.isPost("https://www.instagram.com/creator/p/abc/"))
        assertFalse(InstagramSession.isPost("https://www.instagram.com/accounts/login/"))
        assertFalse(InstagramSession.isPost("https://www.instagram.com/creator/"))
    }

    @Test fun sessionParsingKeepsOnlyNeededNamesAndRejectsHeaderInjection() {
        assertEquals(mapOf("sessionid" to "test=token", "csrftoken" to "csrf"),
            InstagramSession.parseCookies("sessionid=test=token; irrelevant=secret; csrftoken=csrf; ds_user_id="))
        assertTrue(InstagramSession.parseCookies("sessionid=test\r\nAuthorization: bad").isEmpty())
        assertTrue(InstagramSession.parseCookies("sessionid=" + "x".repeat(16_384)).isEmpty())
        assertTrue(InstagramSession.parseCookies(null).isEmpty())
    }

    @Test fun reelLinksUseTheSession() {
        assertTrue(InstagramSession.isPost("https://www.instagram.com/reel/DfDFm7uvDge/"))
        assertTrue(InstagramSession.isPost("https://www.instagram.com/reels/DfDFm7uvDge/"))
    }

    @Test fun cookieFileIsSecureInstagramScopedNetscapeFormat() {
        val file = instagramNetscapeCookies(mapOf("sessionid" to "abc%3A123", "csrftoken" to "tok"))
        assertEquals(
            "# Netscape HTTP Cookie File\n" +
                ".instagram.com\tTRUE\t/\tTRUE\t0\tsessionid\tabc%3A123\n" +
                ".instagram.com\tTRUE\t/\tTRUE\t0\tcsrftoken\ttok\n",
            file,
        )
    }

    @Test fun cookieFileDropsValuesThatCouldBreakItsFormat() {
        val file = instagramNetscapeCookies(mapOf("sessionid" to "ok", "csrftoken" to "a\tb", "mid" to "line\nbreak", "rur" to ""))
        assertEquals("# Netscape HTTP Cookie File\n.instagram.com\tTRUE\t/\tTRUE\t0\tsessionid\tok\n", file)
        assertNull(instagramNetscapeCookies(emptyMap()))
        assertNull(instagramNetscapeCookies(mapOf("sessionid" to "bad value")))
    }

    @Test fun signedOutReelFailuresOfferSignInOnlyForInstagramPosts() {
        val emptyMedia = java.io.IOException("ERROR: [Instagram] DfDFm7uvDge: Instagram sent an empty media response. Check if this post is accessible")
        assertTrue(isInstagramSignInFailure("https://www.instagram.com/reel/DfDFm7uvDge/", emptyMedia))
        assertTrue(isInstagramSignInFailure("https://www.instagram.com/p/abc/", java.io.IOException("use --cookies-from-browser or --cookies")))
        assertFalse(isInstagramSignInFailure("https://www.instagram.com/reel/abc/", java.io.IOException("HTTP Error 404: Not Found")))
        assertFalse(isInstagramSignInFailure("https://www.youtube.com/watch?v=abc", emptyMedia))
    }
}
