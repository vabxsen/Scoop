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
}
