package com.scoop.app.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistUrlTest {
    @Test
    fun recognizesPureYouTubePlaylists() {
        assertTrue(isPlaylistUrl("https://www.youtube.com/playlist?list=PL123"))
        assertTrue(isPlaylistUrl("https://m.youtube.com/playlist?list=PL123"))
        assertTrue(isPlaylistUrl("https://music.youtube.com/playlist?list=OLAK5uy"))
        assertTrue(isPlaylistUrl("https://youtube.com/watch?list=PL123"))
    }

    @Test
    fun keepsSingleVideosFromInsidePlaylists() {
        assertFalse(isPlaylistUrl("https://www.youtube.com/watch?v=abc&list=PL123"))
        assertFalse(isPlaylistUrl("https://youtu.be/abc?list=PL123"))
        assertFalse(isPlaylistUrl("https://www.youtube.com/shorts/abc?list=PL123"))
        assertFalse(isPlaylistUrl("https://www.youtube.com/embed/abc?list=PL123"))
    }

    @Test
    fun ignoresListParametersOnOtherSites() {
        assertFalse(isPlaylistUrl("https://example.com/video.php?list=5"))
        assertFalse(isPlaylistUrl("https://example.com/playlist?list=5"))
        assertFalse(isPlaylistUrl("not a url"))
    }
}
