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

        val url3 = "https://youtu.be/0mJSNgnl8y8?is=0wBKMweqBeC3ABen"
        assertEquals("0mJSNgnl8y8", DeepLinkUtils.extractYouTubeId(url3))
    }

    @Test
    fun toneEngine_synthesizesPresetWithExactSongTitle() {
        val title = "Eli Soares - Se Eu Cair"
        val preset = com.example.data.remote.Mk20ToneEngine.synthesizePresetLocally(
            songOrStyle = title,
            youtubeUrl = "https://youtu.be/0mJSNgnl8y8",
            details = "Clean com chorus suave"
        )
        assertEquals(title, preset.title)
    }

    @Test
    fun masterSongDatabase_findsClassicTracksWithStudioCalibration() {
        val match = com.example.data.model.Mk20MasterSongDatabase.findSong("sweet child")
        assertNotNull(match)
        assertEquals("Sweet Child O' Mine", match?.title)
        assertEquals("51OD1_MS2203", match?.preset?.pedals?.AMP?.model)
        assertEquals(6800f, match?.preset?.pedals?.CAB?.params?.get("High Cut"))
        assertEquals(80f, match?.preset?.pedals?.CAB?.params?.get("Low Cut"))
    }

    @Test
    fun toneEngine_synthesizesMasterPresetDirectly() {
        val preset = com.example.data.remote.Mk20ToneEngine.synthesizePresetLocally(
            songOrStyle = "Master of Puppets",
            youtubeUrl = "",
            details = ""
        )
        assertEquals("Master of Puppets", preset.title)
        assertEquals("48CH1_ME", preset.pedals.AMP?.model)
        assertEquals(6500f, preset.pedals.CAB?.params?.get("High Cut"))
    }

    @Test
    fun spectralResponseCalculator_comparesGeneratedToneAgainstTargetOriginal() {
        val masterPreset = com.example.data.model.Mk20MasterSongDatabase.findBestMatch("Sweet Child O' Mine")!!
        val analysis = com.example.ui.components.SpectralResponseCalculator.analyzeComparison(
            currentPreset = masterPreset,
            songInput = "Sweet Child O' Mine"
        )
        assertEquals(72, analysis.frequencies.size)
        assertEquals(72, analysis.generatedCurveDb.size)
        assertEquals(72, analysis.targetCurveDb.size)
        assertTrue(analysis.matchPercentage >= 95)
        assertEquals(6, analysis.diagnostics.size)
    }

    @Test
    fun binary412Serialization_matchesHardwareSpec() {
        val defaultPreset = Mk20Preset.DEFAULT_PRESET
        val bytes = defaultPreset.toBinaryMk20()
        assertEquals(412, bytes.size)

        // FX Model "Compress" is #9 in hardware -> byte 8 at offset 0x32 (NOT 0 which is Wah-Wah!)
        assertEquals(8, bytes[0x32].toInt() and 0xFF)

        // EQ bands at 0xB4 are signed shorts in -12..+12 dB
        val buffer = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        assertEquals(0, buffer.getShort(0xB4).toInt())   // 100Hz = 0dB
        assertEquals(-2, buffer.getShort(0xB8).toInt())  // 400Hz = -2dB
        assertEquals(2, buffer.getShort(0xBC).toInt())   // 1.6kHz = +2dB

        // CAB High Cut 7500Hz -> 75 at 0xA0
        assertEquals(75, buffer.getShort(0xA0).toInt())

        // Round-trip deserialization preserves models and values
        val deserialized = Mk20Preset.fromBinary(bytes)
        assertEquals("Compress", deserialized.pedals.FX?.model)
        assertEquals(-2f, deserialized.pedals.EQ?.params?.get("400Hz"))
    }

    @Test
    fun officialMatrixLock_preservesVolumeAndBrillianceForGospelSolo() {
        val preset = com.example.data.remote.Mk20ToneEngine.synthesizePresetLocally(
            songOrStyle = "O Escudo - Voz da Verdade",
            youtubeUrl = "",
            details = ""
        )
        assertTrue(preset.masterVolume in 60..70)
        assertEquals("86OD_JCM800", preset.pedals.AMP?.model)
        assertEquals("33Mar60A412A", preset.pedals.CAB?.model)
        assertEquals(-52f, preset.pedals.NS?.params?.get("thd"))
        assertTrue((preset.pedals.AMP?.params?.get("Bright") ?: 0f) >= 65f)
        assertEquals(7800f, preset.pedals.CAB?.params?.get("High Cut"))
    }
}
