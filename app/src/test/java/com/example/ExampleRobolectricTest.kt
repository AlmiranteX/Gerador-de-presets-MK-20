package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Preset20 AI", appName)
  }

  @Test
  fun `pcmToWav builds valid WAV header`() {
    val recorder = com.example.util.AudioVibeRecorder()
    val fakePcm = ByteArray(3200) // 100ms at 16kHz 16-bit
    val wav = recorder.pcmToWav(fakePcm)

    assertEquals(3200 + 44, wav.size)
    // Check RIFF header
    assertEquals('R'.code.toByte(), wav[0])
    assertEquals('I'.code.toByte(), wav[1])
    assertEquals('F'.code.toByte(), wav[2])
    assertEquals('F'.code.toByte(), wav[3])
    // Check WAVE format
    assertEquals('W'.code.toByte(), wav[8])
    assertEquals('A'.code.toByte(), wav[9])
    assertEquals('V'.code.toByte(), wav[10])
    assertEquals('E'.code.toByte(), wav[11])
  }

  @Test
  fun `analyzeAcousticVibe extracts tone characteristics`() {
    val recorder = com.example.util.AudioVibeRecorder()
    // Test empty PCM fallback
    val emptyVibe = recorder.analyzeAcousticVibe(ByteArray(0))
    org.junit.Assert.assertNotNull(emptyVibe.suggestedAmp)
    org.junit.Assert.assertNotNull(emptyVibe.suggestedCab)

    // Test generated high-gain signal (high amplitude, high-frequency square wave)
    val highGainPcm = ByteArray(16000 * 2)
    val byteBuffer = java.nio.ByteBuffer.wrap(highGainPcm).order(java.nio.ByteOrder.LITTLE_ENDIAN)
    for (i in 0 until 16000) {
      val sample = if ((i / 4) % 2 == 0) 12000.toShort() else (-12000).toShort()
      byteBuffer.putShort(sample)
    }

    val highGainVibe = recorder.analyzeAcousticVibe(highGainPcm)
    org.junit.Assert.assertTrue(highGainVibe.rmsEnergy > 5000f)
    org.junit.Assert.assertTrue(highGainVibe.noiseGateActive)
    assertEquals("75DS_ENGL_120", highGainVibe.suggestedAmp)
  }

  @Test
  fun `generatePresetFromAudio builds valid MK20 preset`() = kotlinx.coroutines.runBlocking {
    val recorder = com.example.util.AudioVibeRecorder()
    val vibe = recorder.analyzeAcousticVibe(ByteArray(0)).copy(
      title = "Meu Timbre Vibe Especial"
    )

    val result = com.example.data.remote.Mk20ToneEngine.generatePresetFromAudio(vibe, customApiKey = null)
    org.junit.Assert.assertTrue(result.isSuccess)
    val preset = result.getOrThrow()

    assertEquals("Meu Timbre Vibe Especial", preset.title)
    org.junit.Assert.assertNotNull(preset.pedals.AMP)
    org.junit.Assert.assertNotNull(preset.pedals.CAB)
    org.junit.Assert.assertNotNull(preset.pedals.DS)
    org.junit.Assert.assertNotNull(preset.pedals.DELAY)
    org.junit.Assert.assertNotNull(preset.pedals.REVERB)
  }

  @Test
  fun `searchSongs returns 3 to 5 options for I Will Not Bow with artist title and album`() = kotlinx.coroutines.runBlocking {
    val results = com.example.data.remote.SongSearchService.searchSongs("I Will Not Bow")
    org.junit.Assert.assertTrue("Should return at least 3 options", results.size in 3..5)

    val first = results.first()
    assertEquals("Breaking Benjamin", first.artist)
    assertEquals("I Will Not Bow", first.title)
    org.junit.Assert.assertTrue(first.album.isNotBlank())
    org.junit.Assert.assertTrue("Album should be Dear Agony or collection", first.album.contains("Dear Agony") || first.album.contains("Shallow Bay"))
  }

  @Test
  fun `selectSongSuggestion populates confirmed track in ViewModel`() {
    val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.ui.viewmodel.PresetViewModel(context)

    val suggestion = com.example.data.remote.SongSuggestion(
      artist = "Breaking Benjamin",
      title = "I Will Not Bow",
      album = "Dear Agony",
      version = "Studio",
      genre = "Alternative Metal"
    )

    vm.selectSongSuggestion(suggestion)

    assertEquals("Breaking Benjamin - I Will Not Bow", vm.songInput.value)
    assertEquals(suggestion, vm.selectedSongSuggestion.value)
    org.junit.Assert.assertTrue(vm.detailsInput.value.contains("Dear Agony"))
  }

  @Test
  fun `Mk20BinarySerializer serializes and deserializes with byte-level fidelity`() {
    val original = com.example.data.model.Mk20Preset.DEFAULT_PRESET.copy(
      title = "Breaking Benjamin",
      style = "Modern Metal",
      bpm = 135,
      masterVolume = 42
    )

    // 1. Serialize to binary .mk20 (412 bytes authentic MK-20 standard)
    val bytes = original.toBinaryMk20()
    assertEquals(com.example.util.Mk20BinarySerializer.AUTHENTIC_MK20_SIZE, bytes.size)
    assertEquals(412, bytes.size)

    org.junit.Assert.assertTrue(com.example.util.Mk20BinarySerializer.isBinaryPreset(bytes))

    // 2. Deserialize back
    val restored = com.example.util.Mk20BinarySerializer.deserialize(bytes)
    assertEquals("Breaking Benjamin", restored.title)
    assertEquals(135, restored.bpm)
    assertEquals(42, restored.masterVolume)

    // Verify Modules are present and active
    org.junit.Assert.assertNotNull(restored.pedals.AMP)
    org.junit.Assert.assertNotNull(restored.pedals.DS)
    org.junit.Assert.assertNotNull(restored.pedals.DELAY)
    org.junit.Assert.assertNotNull(restored.pedals.REVERB)
    org.junit.Assert.assertNotNull(restored.pedals.FX)
    org.junit.Assert.assertNotNull(restored.pedals.NS)
    org.junit.Assert.assertNotNull(restored.pedals.EQ)
    org.junit.Assert.assertNotNull(restored.pedals.VOL)

    // Verify parameter restoration
    assertEquals(28f, restored.pedals.AMP?.params?.get("Gain"))
    assertEquals(340f, restored.pedals.DELAY?.params?.get("Time"))
  }

  @Test
  fun `Mk20BinarySerializer supports multi-size profiles and template cloning`() {
    val preset = com.example.data.model.Mk20Preset.DEFAULT_PRESET.copy(
      title = "Tamanho Teste",
      bpm = 80,
      masterVolume = 40
    )

    // 412 bytes authentic
    val bytes412 = preset.toBinaryMk20(targetSize = com.example.util.Mk20BinarySerializer.AUTHENTIC_MK20_SIZE)
    assertEquals(412, bytes412.size)

    // 256 bytes compact
    val bytes256 = preset.toBinaryMk20(targetSize = com.example.util.Mk20BinarySerializer.SIZE_256)
    assertEquals(256, bytes256.size)

    // 512 bytes legacy
    val bytes512 = preset.toBinaryMk20(targetSize = com.example.util.Mk20BinarySerializer.SIZE_512)
    assertEquals(512, bytes512.size)

    // 1024 bytes expanded
    val bytes1024 = preset.toBinaryMk20(targetSize = com.example.util.Mk20BinarySerializer.SIZE_1024)
    assertEquals(1024, bytes1024.size)

    // Template cloning test (412 bytes)
    val fakeTemplate = ByteArray(412) { 0x55.toByte() }
    val cloned = com.example.util.Mk20BinarySerializer.applyToTemplate(fakeTemplate, preset)
    assertEquals(412, cloned.size)
  }

  @Test
  fun `PresetViewModel loads binary preset from bytes`() {
    val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.ui.viewmodel.PresetViewModel(context)

    val custom = com.example.data.model.Mk20Preset(
      title = "Solo Hendrix",
      style = "Psychedelic Rock",
      bpm = 112,
      masterVolume = 40
    )
    val binaryData = custom.toBinaryMk20()

    vm.importPresetFromBytes(binaryData, "solo.mk20")

    assertEquals("Solo Hendrix", vm.currentPreset.value.title)
    assertEquals(112, vm.currentPreset.value.bpm)
    assertEquals(412, vm.getExportBytes().size)
  }

  @Test
  fun `Mk20Catalog contains exact official model count from PDF manual`() {
    assertEquals(120, com.example.data.model.Mk20Catalog.AMP_MODELS.size)
    assertEquals(100, com.example.data.model.Mk20Catalog.CAB_MODELS.size)
    assertEquals(52, com.example.data.model.Mk20Catalog.DS_MODELS.size)
    assertEquals(20, com.example.data.model.Mk20Catalog.FX_MODELS.size)
    assertEquals(8, com.example.data.model.Mk20Catalog.NS_MODELS.size)
    assertEquals(3, com.example.data.model.Mk20Catalog.EQ_MODELS.size)
    assertEquals(20, com.example.data.model.Mk20Catalog.MOD_MODELS.size)
    assertEquals(27, com.example.data.model.Mk20Catalog.DELAY_MODELS.size)
    assertEquals(18, com.example.data.model.Mk20Catalog.REVERB_MODELS.size)

    assertEquals("1CL1_410H", com.example.data.model.Mk20Catalog.findAmp(1)?.name)
    assertEquals("120GKL800_BS", com.example.data.model.Mk20Catalog.findAmp(120)?.name)
    assertEquals("1BogUber412A", com.example.data.model.Mk20Catalog.findCab(1)?.name)
    assertEquals("100TaceEll118", com.example.data.model.Mk20Catalog.findCab(100)?.name)
    assertEquals("MW-808", com.example.data.model.Mk20Catalog.findDs(1)?.name)
    assertEquals("MW-SD1", com.example.data.model.Mk20Catalog.findDs(4)?.name)
    assertEquals("MW-B7000", com.example.data.model.Mk20Catalog.findDs(12)?.name)
    assertEquals("1._ADAM_RAFOW", com.example.data.model.Mk20Catalog.findDs(13)?.name)
    assertEquals("7M-VAVE_TS1", com.example.data.model.Mk20Catalog.findDs(19)?.name)
    assertEquals("SLASH_USE_YOU", com.example.data.model.Mk20Catalog.findDs(52)?.name)
    assertEquals("Guitar EQ 6", com.example.data.model.Mk20Catalog.findEq(1)?.name)
    assertEquals("Bass EQ 7", com.example.data.model.Mk20Catalog.findEq(2)?.name)
    assertEquals("Normal EQ 10", com.example.data.model.Mk20Catalog.findEq(3)?.name)
    assertEquals("CHORUS", com.example.data.model.Mk20Catalog.findMod(1)?.name)
    assertEquals("TRI CHORUS", com.example.data.model.Mk20Catalog.findMod(2)?.name)
    assertEquals("AUTOFILTER", com.example.data.model.Mk20Catalog.findMod(14)?.name)
    assertEquals("PHASER STEREO", com.example.data.model.Mk20Catalog.findMod(15)?.name)
    assertEquals("FLANGER STEREO", com.example.data.model.Mk20Catalog.findMod(16)?.name)
    assertEquals("VIBRATO STEREO", com.example.data.model.Mk20Catalog.findMod(20)?.name)
    assertEquals("Clean", com.example.data.model.Mk20Catalog.findDelay(1)?.name)
    assertEquals("Tremolo", com.example.data.model.Mk20Catalog.findDelay(7)?.name)
    assertEquals("PingPong Stereo", com.example.data.model.Mk20Catalog.findDelay(14)?.name)
    assertEquals("Clean Stereo", com.example.data.model.Mk20Catalog.findDelay(15)?.name)
    assertEquals("Tremolo Stereo", com.example.data.model.Mk20Catalog.findDelay(21)?.name)
    assertEquals("Reverse Stereo", com.example.data.model.Mk20Catalog.findDelay(27)?.name)
    assertEquals("room", com.example.data.model.Mk20Catalog.findReverb(1)?.name)
    assertEquals("shimmer", com.example.data.model.Mk20Catalog.findReverb(5)?.name)
    assertEquals("cloud", com.example.data.model.Mk20Catalog.findReverb(7)?.name)
    assertEquals("lofi", com.example.data.model.Mk20Catalog.findReverb(8)?.name)
    assertEquals("swell", com.example.data.model.Mk20Catalog.findReverb(9)?.name)
    assertEquals("room stereo", com.example.data.model.Mk20Catalog.findReverb(10)?.name)
    assertEquals("swell streo", com.example.data.model.Mk20Catalog.findReverb(18)?.name)
  }

  @Test
  fun `AMP CAB EQ MOD DELAY and REVERB serialize and deserialize all Firmware v85 parameters`() {
    val preset = com.example.data.model.Mk20Preset.DEFAULT_PRESET.copy(
      pedals = com.example.data.model.Mk20Preset.DEFAULT_PRESET.pedals.copy(
        AMP = com.example.data.model.PedalModule(
          name = "Amplificador",
          active = true,
          model = "85DS_DiezzIVH4",
          params = mapOf(
            "Gain" to 78f, "Level" to 72f, "Bass" to 58f, "Middle" to 46f,
            "Treble" to 64f, "Reso" to 72f, "Pres" to 68f, "Bright" to 15f
          )
        ),
        CAB = com.example.data.model.PedalModule(
          name = "Caixa / IR",
          active = true,
          model = "24GojaCabA",
          params = mapOf("Level" to 84f, "Low Cut" to 95f, "High Cut" to 6500f)
        ),
        EQ = com.example.data.model.PedalModule(
          name = "Equalizador",
          active = true,
          model = "Normal EQ 10",
          params = mapOf(
            "31.25Hz" to -2f, "62.5Hz" to 1f, "125Hz" to 2f, "250Hz" to -2f, "500Hz" to -1f,
            "1kHz" to 1f, "2kHz" to 3f, "4kHz" to 4f, "8kHz" to 2f, "16kHz" to -1f
          )
        ),
        MOD = com.example.data.model.PedalModule(
          name = "Modulação",
          active = true,
          model = "FLANGER STEREO",
          params = mapOf(
            "speed" to 1.0f, "depth" to 50f, "regen" to 75f, "mix" to 50f,
            "jet" to 20f, "lfo" to 1f, "sync" to 1f, "syncBpm" to 120f
          )
        ),
        DELAY = com.example.data.model.PedalModule(
          name = "Delay",
          active = true,
          model = "Tremolo Stereo",
          params = mapOf(
            "Time" to 500f, "Fb" to 30f, "Mix" to 50f, "Pattern" to 1f,
            "Dive" to 30f, "Speed" to 15f, "Depth" to 50f, "Sync" to 0f, "SyncBPM" to 80f
          )
        ),
        REVERB = com.example.data.model.PedalModule(
          name = "Reverb",
          active = true,
          model = "lofi stereo",
          params = mapOf(
            "decay" to 20f, "mix" to 30f, "sampleRete" to 80f, "noiseLevel" to 40f, "modDepth" to 50f
          )
        )
      )
    )

    val bytes = preset.toBinaryMk20()
    assertEquals(412, bytes.size)

    val restored = com.example.util.Mk20BinarySerializer.deserialize(bytes)
    assertEquals("85DS_DiezzIVH4", restored.pedals.AMP?.model)
    assertEquals(78f, restored.pedals.AMP?.params?.get("Gain"))
    assertEquals(72f, restored.pedals.AMP?.params?.get("Reso"))
    assertEquals(15f, restored.pedals.AMP?.params?.get("Bright"))

    assertEquals("24GojaCabA", restored.pedals.CAB?.model)
    assertEquals(84f, restored.pedals.CAB?.params?.get("Level"))
    assertEquals(95f, restored.pedals.CAB?.params?.get("Low Cut"))
    assertEquals(6500f, restored.pedals.CAB?.params?.get("High Cut"))

    assertEquals("Normal EQ 10", restored.pedals.EQ?.model)
    assertEquals(-2f, restored.pedals.EQ?.params?.get("31.25Hz"))
    assertEquals(4f, restored.pedals.EQ?.params?.get("4kHz"))
    assertEquals(-1f, restored.pedals.EQ?.params?.get("16kHz"))

    assertEquals("FLANGER STEREO", restored.pedals.MOD?.model)
    assertEquals(1.0f, restored.pedals.MOD?.params?.get("speed"))
    assertEquals(75f, restored.pedals.MOD?.params?.get("regen"))
    assertEquals(20f, restored.pedals.MOD?.params?.get("jet"))
    assertEquals(1f, restored.pedals.MOD?.params?.get("lfo"))
    assertEquals(1f, restored.pedals.MOD?.params?.get("sync"))
    assertEquals(120f, restored.pedals.MOD?.params?.get("syncBpm"))

    assertEquals("Tremolo Stereo", restored.pedals.DELAY?.model)
    assertEquals(500f, restored.pedals.DELAY?.params?.get("Time"))
    assertEquals(30f, restored.pedals.DELAY?.params?.get("Fb"))
    assertEquals(50f, restored.pedals.DELAY?.params?.get("Mix"))
    assertEquals(1f, restored.pedals.DELAY?.params?.get("Pattern"))
    assertEquals(30f, restored.pedals.DELAY?.params?.get("Dive"))
    assertEquals(15f, restored.pedals.DELAY?.params?.get("Speed"))
    assertEquals(50f, restored.pedals.DELAY?.params?.get("Depth"))
    assertEquals(0f, restored.pedals.DELAY?.params?.get("Sync"))
    assertEquals(80f, restored.pedals.DELAY?.params?.get("SyncBPM"))

    assertEquals("lofi stereo", restored.pedals.REVERB?.model)
    assertEquals(20f, restored.pedals.REVERB?.params?.get("decay"))
    assertEquals(30f, restored.pedals.REVERB?.params?.get("mix"))
    assertEquals(80f, restored.pedals.REVERB?.params?.get("sampleRete"))
    assertEquals(40f, restored.pedals.REVERB?.params?.get("noiseLevel"))
    assertEquals(50f, restored.pedals.REVERB?.params?.get("modDepth"))
  }

  @Test
  fun `DS module serializes and deserializes all 8 equalization controls`() {
    val customDs = com.example.data.model.PedalModule(
      name = "Drive / Boost",
      active = true,
      model = "2TS8",
      params = mapOf(
        "gain" to 50f,
        "level" to 50f,
        "bass" to 50f,
        "middle" to 50f,
        "treble" to 50f,
        "reso" to 0f,
        "pres" to 50f,
        "bright" to 100f
      )
    )

    val preset = com.example.data.model.Mk20Preset.DEFAULT_PRESET.copy(
      pedals = com.example.data.model.Mk20Preset.DEFAULT_PRESET.pedals.copy(
        DS = customDs
      )
    )

    val bytes = preset.toBinaryMk20()
    assertEquals(412, bytes.size)

    val restored = com.example.util.Mk20BinarySerializer.deserialize(bytes)
    val restoredDs = restored.pedals.DS
    org.junit.Assert.assertNotNull(restoredDs)
    assertEquals("2TS8", restoredDs?.model)
    assertEquals(50f, restoredDs?.params?.get("gain"))
    assertEquals(50f, restoredDs?.params?.get("level"))
    assertEquals(50f, restoredDs?.params?.get("bass"))
    assertEquals(50f, restoredDs?.params?.get("middle"))
    assertEquals(50f, restoredDs?.params?.get("treble"))
    assertEquals(0f, restoredDs?.params?.get("reso"))
    assertEquals(50f, restoredDs?.params?.get("pres"))
    assertEquals(100f, restoredDs?.params?.get("bright"))
  }

  @Test
  fun `DS module serializes and deserializes specific parameter models MW-SD1 and MW-B7000`() {
    // 1. MW-SD1 (3 params: gain=000, tone=050, level=100)
    val sd1 = com.example.data.model.PedalModule(
      name = "Drive / Boost",
      active = true,
      model = "MW-SD1",
      params = mapOf("gain" to 0f, "tone" to 50f, "level" to 100f)
    )
    val presetSd1 = com.example.data.model.Mk20Preset.DEFAULT_PRESET.copy(
      pedals = com.example.data.model.Mk20Preset.DEFAULT_PRESET.pedals.copy(DS = sd1)
    )
    val bytesSd1 = presetSd1.toBinaryMk20()
    assertEquals(412, bytesSd1.size)
    val restoredSd1 = com.example.util.Mk20BinarySerializer.deserialize(bytesSd1).pedals.DS
    org.junit.Assert.assertNotNull(restoredSd1)
    assertEquals("MW-SD1", restoredSd1?.model)
    assertEquals(0f, restoredSd1?.params?.get("gain"))
    assertEquals(50f, restoredSd1?.params?.get("tone"))
    assertEquals(100f, restoredSd1?.params?.get("level"))

    // 2. MW-B7000 (6 params: gain=050, level=050, blend=050, grunt=2/4, attack=2/3, master=050)
    val b7k = com.example.data.model.PedalModule(
      name = "Drive / Boost",
      active = true,
      model = "MW-B7000",
      params = mapOf(
        "gain" to 50f, "level" to 50f, "blend" to 50f, "grunt" to 2f,
        "attack" to 2f, "master" to 50f
      )
    )
    val presetB7k = com.example.data.model.Mk20Preset.DEFAULT_PRESET.copy(
      pedals = com.example.data.model.Mk20Preset.DEFAULT_PRESET.pedals.copy(DS = b7k)
    )
    val bytesB7k = presetB7k.toBinaryMk20()
    assertEquals(412, bytesB7k.size)
    val restoredB7k = com.example.util.Mk20BinarySerializer.deserialize(bytesB7k).pedals.DS
    org.junit.Assert.assertNotNull(restoredB7k)
    assertEquals("MW-B7000", restoredB7k?.model)
    assertEquals(50f, restoredB7k?.params?.get("gain"))
    assertEquals(50f, restoredB7k?.params?.get("level"))
    assertEquals(50f, restoredB7k?.params?.get("blend"))
    assertEquals(2f, restoredB7k?.params?.get("grunt"))
    assertEquals(2f, restoredB7k?.params?.get("attack"))
    assertEquals(50f, restoredB7k?.params?.get("master"))
  }

  @Test
  fun `Mk20Catalog contains exact 20 official FX models in manual order`() {
    assertEquals(20, com.example.data.model.Mk20Catalog.FX_MODELS.size)
    assertEquals("Wah-Wah", com.example.data.model.Mk20Catalog.FX_MODELS[0].name)
    assertEquals("AUTO / LFO WAH", com.example.data.model.Mk20Catalog.FX_MODELS[0].type)
    assertEquals("Compress", com.example.data.model.Mk20Catalog.FX_MODELS[8].name)
    assertEquals("COMPRESSOR", com.example.data.model.Mk20Catalog.FX_MODELS[8].type)
    assertEquals("Cry-Wah", com.example.data.model.Mk20Catalog.findFx("Cry-Wah")?.name)
    assertEquals("CLASSIC WAH", com.example.data.model.Mk20Catalog.findFx("Cry-Wah")?.type)

    val wah = com.example.data.model.Mk20Catalog.findFx("Wah-Wah")
    org.junit.Assert.assertNotNull(wah)
    assertEquals(2.5f, wah?.defaultParams?.get("Speed"))
    assertEquals(50f, wah?.defaultParams?.get("Q"))
    assertEquals(80f, wah?.defaultParams?.get("Mix"))
    assertEquals(25f, wah?.defaultParams?.get("Width"))
    assertEquals(50f, wah?.defaultParams?.get("Level"))
    assertEquals(0f, wah?.defaultParams?.get("Sync"))
    assertEquals(80f, wah?.defaultParams?.get("SyncBpm"))

    val whammy = com.example.data.model.Mk20Catalog.findFx("Whammy")
    org.junit.Assert.assertNotNull(whammy)
    assertEquals(3, whammy?.defaultParams?.size)
    assertEquals(2f, whammy?.defaultParams?.get("mode")) // oct1
    assertEquals(0f, whammy?.defaultParams?.get("bend")) // 000
    assertEquals(100f, whammy?.defaultParams?.get("Mix"))

    assertEquals("Whammy", com.example.data.model.Mk20Catalog.FX_MODELS[19].name)
  }

  @Test
  fun `FX module serializes and deserializes accurately in 412-byte preset`() {
    val customFx = com.example.data.model.PedalModule(
      name = "Pré-Efeito / Dinâmica",
      active = true,
      model = "Wah-Wah",
      params = mapOf(
        "Speed" to 1.0f,
        "Q" to 50f,
        "Mix" to 80f,
        "Width" to 25f,
        "Level" to 50f,
        "Sync" to 0f,
        "SyncBpm" to 80f
      )
    )

    val preset = com.example.data.model.Mk20Preset.DEFAULT_PRESET.copy(
      pedals = com.example.data.model.Mk20Preset.DEFAULT_PRESET.pedals.copy(
        FX = customFx
      )
    )

    val bytes = preset.toBinaryMk20()
    assertEquals(412, bytes.size)

    val restored = com.example.util.Mk20BinarySerializer.deserialize(bytes)
    val restoredFx = restored.pedals.FX
    org.junit.Assert.assertNotNull(restoredFx)
    assertEquals("Wah-Wah", restoredFx?.model)
    assertEquals(true, restoredFx?.active)
    assertEquals(1.0f, restoredFx?.params?.get("Speed"))
    assertEquals(50f, restoredFx?.params?.get("Q"))
    assertEquals(80f, restoredFx?.params?.get("Mix"))
    assertEquals(25f, restoredFx?.params?.get("Width"))
    assertEquals(50f, restoredFx?.params?.get("Level"))
    assertEquals(0f, restoredFx?.params?.get("Sync"))
    assertEquals(80f, restoredFx?.params?.get("SyncBpm"))
  }

  @Test
  fun `Mk20Catalog contains exact 8 official NS models in manual order`() {
    assertEquals(8, com.example.data.model.Mk20Catalog.NS_MODELS.size)
    assertEquals("ai gate", com.example.data.model.Mk20Catalog.NS_MODELS[0].name)
    assertEquals("ADAPTIVE NEURAL", com.example.data.model.Mk20Catalog.NS_MODELS[0].type)
    assertEquals("ai ms gate", com.example.data.model.Mk20Catalog.NS_MODELS[1].name)
    assertEquals("soft gate", com.example.data.model.Mk20Catalog.findNs("soft gate")?.name)
    assertEquals("SMOOTH DECAY", com.example.data.model.Mk20Catalog.findNs("soft gate")?.type)
    assertEquals(-60f, com.example.data.model.Mk20Catalog.findNs("soft gate")?.defaultParams?.get("thd"))
    assertEquals(0f, com.example.data.model.Mk20Catalog.findNs("soft gate")?.defaultParams?.get("inputDetect"))
    assertEquals("hard gate", com.example.data.model.Mk20Catalog.findNs(4)?.name)
    assertEquals("pro gate", com.example.data.model.Mk20Catalog.findNs(5)?.name)
    assertEquals("compress", com.example.data.model.Mk20Catalog.findNs(6)?.name)
    assertEquals("compress pro", com.example.data.model.Mk20Catalog.findNs(7)?.name)
    assertEquals("f compress", com.example.data.model.Mk20Catalog.NS_MODELS[7].name)
    assertEquals(60f, com.example.data.model.Mk20Catalog.findNs("f compress")?.defaultParams?.get("tone"))
  }

  @Test
  fun `NS module serializes and deserializes accurately in 412-byte preset`() {
    val customNs = com.example.data.model.PedalModule(
      name = "Noise Suppressor",
      active = true,
      model = "pro gate",
      params = mapOf(
        "att" to 50f,
        "rel" to 50f,
        "thd" to -50f,
        "kw" to 50f,
        "ratio" to 50f,
        "inputDetect" to 0f
      )
    )

    val preset = com.example.data.model.Mk20Preset.DEFAULT_PRESET.copy(
      pedals = com.example.data.model.Mk20Preset.DEFAULT_PRESET.pedals.copy(
        NS = customNs
      )
    )

    val bytes = preset.toBinaryMk20()
    assertEquals(412, bytes.size)

    val restored = com.example.util.Mk20BinarySerializer.deserialize(bytes)
    val restoredNs = restored.pedals.activeNS
    org.junit.Assert.assertNotNull(restoredNs)
    assertEquals("pro gate", restoredNs?.model)
    assertEquals(true, restoredNs?.active)
    assertEquals(50f, restoredNs?.params?.get("att"))
    assertEquals(50f, restoredNs?.params?.get("rel"))
    assertEquals(-50f, restoredNs?.params?.get("thd"))
    assertEquals(50f, restoredNs?.params?.get("kw"))
    assertEquals(50f, restoredNs?.params?.get("ratio"))
    assertEquals(0f, restoredNs?.params?.get("inputDetect"))
  }

  @Test
  fun `MOD module contains exact 20 models and serializes all mod_eb8243f9 parameters`() {
    assertEquals(20, com.example.data.model.Mk20Catalog.MOD_MODELS.size)
    assertEquals("CHORUS", com.example.data.model.Mk20Catalog.MOD_MODELS[0].name)
    assertEquals(1.2f, com.example.data.model.Mk20Catalog.findMod("TRI CHORUS")?.defaultParams?.get("speed"))
    assertEquals(50f, com.example.data.model.Mk20Catalog.findMod("AUTOFILTER")?.defaultParams?.get("fb"))
    assertEquals(1f, com.example.data.model.Mk20Catalog.findMod("PHASER STEREO")?.defaultParams?.get("lfo"))
    assertEquals(5f, com.example.data.model.Mk20Catalog.findMod("VIBE STEREO")?.defaultParams?.get("lfo"))
    assertEquals(1f, com.example.data.model.Mk20Catalog.findMod("CHORUS STEREO")?.defaultParams?.get("mode"))
    assertEquals(1f, com.example.data.model.Mk20Catalog.findMod("TREMOLO STEREO")?.defaultParams?.get("mode"))
    assertEquals(80f, com.example.data.model.Mk20Catalog.findMod("VIBRATO STEREO")?.defaultParams?.get("bits"))

    val customMod = com.example.data.model.PedalModule(
      name = "Modulação",
      active = true,
      model = "VIBRATO STEREO",
      params = mapOf(
        "speed" to 1.0f,
        "depth" to 50f,
        "bits" to 20f,
        "mix" to 50f,
        "sync" to 0f,
        "syncBpm" to 80f
      )
    )
    val preset = com.example.data.model.Mk20Preset.DEFAULT_PRESET.copy(
      pedals = com.example.data.model.Mk20Preset.DEFAULT_PRESET.pedals.copy(MOD = customMod)
    )
    val bytes = preset.toBinaryMk20()
    assertEquals(412, bytes.size)
    val restoredMod = com.example.util.Mk20BinarySerializer.deserialize(bytes).pedals.MOD
    org.junit.Assert.assertNotNull(restoredMod)
    assertEquals("VIBRATO STEREO", restoredMod?.model)
    assertEquals(1.0f, restoredMod?.params?.get("speed"))
    assertEquals(50f, restoredMod?.params?.get("depth"))
    assertEquals(20f, restoredMod?.params?.get("bits"))
    assertEquals(50f, restoredMod?.params?.get("mix"))
    assertEquals(0f, restoredMod?.params?.get("sync"))
    assertEquals(80f, restoredMod?.params?.get("syncBpm"))
  }

  @Test
  fun `MK20 binary preset matches authentic hardware signature and null terminated title`() {
    val longTitlePreset = com.example.data.model.Mk20Preset.DEFAULT_PRESET.copy(
      title = "meu_prazer_(em_espírito,_em_verdade)_instrumental_long_title"
    )
    val bytes = longTitlePreset.toBinaryMk20()
    assertEquals(412, bytes.size)

    // Hardware ID 0x001E90FF at offset 0x14..0x17
    assertEquals((0xff).toByte(), bytes[0x14])
    assertEquals((0x90).toByte(), bytes[0x15])
    assertEquals((0x1e).toByte(), bytes[0x16])
    assertEquals((0x00).toByte(), bytes[0x17])

    // Title byte 19 must ALWAYS be 0x00 (null terminator)
    assertEquals(0.toByte(), bytes[19])

    // Magic trailer NGD4 at 405..408
    assertEquals('N'.code.toByte(), bytes[405])
    assertEquals('G'.code.toByte(), bytes[406])
    assertEquals('D'.code.toByte(), bytes[407])
    assertEquals('4'.code.toByte(), bytes[408])

    // Write to /tmp/generated_test.mk20
    java.io.File("/tmp/generated_test.mk20").writeBytes(bytes)
  }

  @Test
  fun `Mk20BinaryDiffer confirms zero structural differences against official M-EFCS puts_mk20 and detects legacy errors`() {
    // 1. Preset com acentos, símbolos UTF-8 e nome longo (como "Abraça-me • Brasas")
    val abracameFixed = com.example.data.model.Mk20Preset.DEFAULT_PRESET.copy(
      title = "ABRAÇA-ME • BRASAS — DAVID QUINLAN & JULLIANY SOUZA",
      bpm = 72,
      masterVolume = 60
    )
    val generatedBytes = abracameFixed.toBinaryMk20()
    val officialPutsBytes = com.example.util.Mk20BinarySerializer.getAuthenticTemplateBytes()

    val report = com.example.util.Mk20BinaryDiffer.compare(
      leftBytes = generatedBytes,
      rightBytes = officialPutsBytes,
      leftLabel = "Abracame Corrigido",
      rightLabel = "puts.mk20 (M-EFCS)"
    )

    // Todas as 8 validações de hardware v85 devem passar 100%
    org.junit.Assert.assertTrue(
      "All validations must pass: ${report.validations.filter { !it.passed }}",
      report.allValidationsPassed
    )
    // Zero diferenças estruturais (Magic ID 0x14..0x17, Ordem 0x1E..0x27, Tabela EXP 0x116..0x193, Trailer NGD4 0x194..0x19B)
    assertEquals(0, report.structuralDiffBytes)
    org.junit.Assert.assertTrue(report.sideBySideHexDump.contains("Magic ID (FF 90 1E 00)"))

    // 2. Simula o arquivo antigo Abracame.mk20 que falhava no M-EFCS (sig 22 00 16 00, título UTF-8 sem null, 0x166=0x09, FX[7]=0)
    val legacyBroken = officialPutsBytes.clone()
    val utf8Title = "ABRAÇA-ME • BRASA".toByteArray(java.nio.charset.StandardCharsets.UTF_8) // 20 bytes exatos sem 0x00!
    System.arraycopy(utf8Title, 0, legacyBroken, 0, 20)
    legacyBroken[0x14] = 0x22.toByte()
    legacyBroken[0x15] = 0x00.toByte()
    legacyBroken[0x16] = 0x16.toByte()
    legacyBroken[0x17] = 0x00.toByte()
    legacyBroken[0x4A] = 0x00.toByte() // FX[7] SyncBpm zerado (inválido, min=40)
    legacyBroken[0x4B] = 0x00.toByte()
    legacyBroken[0x166] = 0x09.toByte() // Byte legado na tabela EXP

    val brokenReport = com.example.util.Mk20BinaryDiffer.compare(
      leftBytes = legacyBroken,
      rightBytes = officialPutsBytes
    )
    org.junit.Assert.assertFalse(brokenReport.allValidationsPassed)
    org.junit.Assert.assertTrue(brokenReport.structuralDiffBytes > 0)
    val failedIds = brokenReport.validations.filter { !it.passed }.map { it.id }
    org.junit.Assert.assertTrue(failedIds.contains("ASCII_TITLE"))
    org.junit.Assert.assertTrue(failedIds.contains("HW_MAGIC"))
    org.junit.Assert.assertTrue(failedIds.contains("PARAM_BOUNDS"))
    org.junit.Assert.assertTrue(failedIds.contains("FOOTER_NGD4"))
  }

  @Test
  fun `toneAssistant adjusts preset for smooth trebles with strato`() {
    val basePreset = com.example.data.model.Mk20Preset.DEFAULT_PRESET
    val result = com.example.data.remote.Mk20ToneAssistantEngine.adjustLocally(
      basePreset,
      "Ficou muito agudo e ardido na minha Strato, tem como amaciar os agudos?"
    )

    org.junit.Assert.assertTrue(result.explanation.contains("Amaciei"))
    org.junit.Assert.assertTrue(result.appliedChanges.isNotEmpty())
    val updated = result.updatedPreset
    val treble = updated.pedals.AMP?.params?.get("Treble") ?: 0f
    val baseTreble = basePreset.pedals.AMP?.params?.get("Treble") ?: 0f
    org.junit.Assert.assertTrue(treble < baseTreble)
    assertEquals(412, updated.toBinaryMk20().size)
  }

  @Test
  fun `toneAssistant adjusts preset for solo lead boost with delay`() {
    val basePreset = com.example.data.model.Mk20Preset.DEFAULT_PRESET
    val result = com.example.data.remote.Mk20ToneAssistantEngine.adjustLocally(
      basePreset,
      "Quero transformar esse timbre para um solo com mais ganho e delay"
    )

    org.junit.Assert.assertTrue(result.explanation.contains("Solo"))
    val updated = result.updatedPreset
    org.junit.Assert.assertTrue(updated.pedals.DELAY?.active == true)
    assertEquals(412, updated.toBinaryMk20().size)
  }

  @Test
  fun `viewModel tone assistant actions and initial message work`() {
    val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.ui.viewmodel.PresetViewModel(context)

    assertEquals(1, vm.toneAssistantMessages.value.size) // Initial greeting message
    org.junit.Assert.assertTrue(vm.toneAssistantMessages.value.first().text.contains("Roadie"))

    // Applying preset to pedalboard
    val customPreset = com.example.data.model.Mk20Preset.DEFAULT_PRESET.copy(title = "Roadie Custom Solo")
    vm.applyAssistantPresetToPedalboard(customPreset)
    assertEquals("Roadie Custom Solo", vm.currentPreset.value.title)
    assertEquals(0, vm.selectedTab.value)

    // Clear conversation resets to greeting
    vm.clearAssistantConversation()
    assertEquals(1, vm.toneAssistantMessages.value.size)
  }

  @Test
  fun `AcousticStudioGuardrail deactivates compressor and tightens noise gate on high gain`() {
    val uncalibratedPreset = com.example.data.model.Mk20Preset(
      title = "Problematic High Gain",
      pedals = com.example.data.model.Mk20Pedals(
        FX = com.example.data.model.PedalModule("Pré-Efeito", true, "Compress", mapOf("Level" to 75f, "Sustain" to 4.8f)),
        DS = com.example.data.model.PedalModule("Drive", true, "MW-808", mapOf("gain" to 55f, "level" to 78f)),
        AMP = com.example.data.model.PedalModule("Amp", true, "13CL1_FenDi", mapOf("Gain" to 66f, "Pres" to 60f, "Treble" to 66f)),
        NS = com.example.data.model.PedalModule("NS", true, "soft gate", mapOf("thd" to -55f)),
        EQ = com.example.data.model.PedalModule("EQ", true, "Guitar EQ 6", mapOf("3.2kHz" to 2f, "1.6kHz" to 3f))
      )
    )

    val sanitized = com.example.util.AcousticStudioGuardrail.sanitize(uncalibratedPreset)

    // 1. Anti-Microfonia: Compressor MUST be deactivated because drive and amp are saturated
    org.junit.Assert.assertFalse("Compressor must be turned off to avoid feedback squeal", sanitized.pedals.FX?.active == true)

    // 2. Anti-Zumbido: Noise Gate must be tightened from -55dB to ~ -48dB to cut pickup hum without choking notes
    val gateThd = sanitized.pedals.NS?.params?.get("thd") ?: -55f
    org.junit.Assert.assertTrue("Noise gate threshold should be tightened to musical level", gateThd >= -52f && gateThd <= -46f)

    // 3. Brilho Musical: EQ 3.2kHz permitted up to 2dB
    val eq32k = sanitized.pedals.EQ?.params?.get("3.2kHz") ?: 0f
    org.junit.Assert.assertTrue("EQ 3.2kHz must allow musical brilliance", eq32k <= 2f)

    // 4. Volume Oficial: Master Volume guaranteed at official stage levels (>= 55)
    org.junit.Assert.assertTrue("Master volume must be at official factory level", sanitized.masterVolume >= 55)
  }
}




