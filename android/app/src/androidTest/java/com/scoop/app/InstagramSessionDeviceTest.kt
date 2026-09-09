package com.scoop.app

import android.webkit.CookieManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.scoop.app.extractor.InstagramSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Assume.assumeFalse
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.coroutines.resume

@RunWith(AndroidJUnit4::class)
class InstagramSessionDeviceTest {
    @Test fun httpOnlySessionIsScopedAndDisconnectRemovesIt() = runBlocking {
        // Never overwrite a developer's actual signed-in debug session.
        assumeFalse(withContext(Dispatchers.Main) { InstagramSession.hasSession() })
        try {
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine<Unit> { continuation ->
                    CookieManager.getInstance().setCookie("https://www.instagram.com/",
                        "sessionid=scoop-local-test-token; Path=/; Secure; HttpOnly") {
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                }
            }
            assertTrue(withContext(Dispatchers.Main) { InstagramSession.hasSession() })
            assertEquals("scoop-local-test-token", InstagramSession.cookiesFor("https://www.instagram.com/p/test/")["sessionid"])
            assertTrue(InstagramSession.cookiesFor("https://example.com/p/test/").isEmpty())
            assertTrue(InstagramSession.cookiesFor("http://www.instagram.com/p/test/").isEmpty())
            InstagramSession.persist()
            InstagramSession.disconnect()
            assertFalse(withContext(Dispatchers.Main) { InstagramSession.hasSession() })
        } finally {
            InstagramSession.disconnect()
        }
    }
}
