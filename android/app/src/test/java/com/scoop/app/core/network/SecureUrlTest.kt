package com.scoop.app.core.network

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SecureUrlTest {
    @Test fun acceptsOnlyHttpsWebUrls() {
        assertNotNull(SecureUrl.parse("https://example.com/media?id=1"))
        listOf("http://example.com/media", "file:///data/local/file", "javascript:alert(1)", "not a url")
            .forEach { assertNull(it, SecureUrl.parse(it)) }
    }
}
