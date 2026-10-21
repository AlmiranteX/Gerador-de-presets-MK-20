package com.example

import android.net.Uri
import com.example.data.model.Mk20Preset
import com.example.util.DeepLinkUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExampleUnitTest {

    @Test
    fun presetSerialization_isAccurate() {
        val original = Mk20Preset.DEFAULT_PRESET
        val json = original.toJsonString()
        val parsed = Mk20Preset.fromJsonString(json)

        assertEquals(original.title, parsed.title)
        assertEquals(original.bpm, parsed.bpm)
        assertEquals(original.pedals.AMP?.model, parsed.pedals.AMP?.model)
        assertEquals(original.pedals.DS?.model, parsed.pedals.DS?.model)
    }

    @Test
    fun youtubeIdExtraction_handlesStandardUrls() {
        val url1 = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        assertEquals("dQw4w9WgXcQ", DeepLinkUtils.extractYouTubeId(url1))

        val url2 = "https://youtu.be/1w7OgIMMRc4"
        assertEquals("1w7OgIMMRc4", DeepLinkUtils.extractYouTubeId(url2))
    }

    @Test
    fun deepLink_roundTrip_succeeds() {
        val preset = Mk20Preset.DEFAULT_PRESET
        val shareUrl = DeepLinkUtils.createShareUrl(preset, useWebScheme = true)
        assertTrue(shareUrl.startsWith("https://mk20tone.app/preset?data="))

        val uri = Uri.parse(shareUrl)
        val decoded = DeepLinkUtils.parsePresetFromUri(uri)
        assertNotNull(decoded)
        assertEquals(preset.title, decoded?.title)
        assertEquals(preset.pedals.AMP?.model, decoded?.pedals?.AMP?.model)
    }
}
