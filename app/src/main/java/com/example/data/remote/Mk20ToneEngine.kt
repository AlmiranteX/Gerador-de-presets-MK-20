package com.example.data.remote

import com.example.BuildConfig
import com.example.data.model.Mk20Pedals
import com.example.data.model.Mk20Preset
import com.example.data.model.PedalModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object Mk20ToneEngine {

    private const val SYSTEM_INSTRUCTION = """
Você é o especialista mestre na pedaleira multi-efeitos M-VAVE / Yuimer MK-20 (Guitar Amp Modeler and Effects Processor).
Sua missão é gerar presets estritamente precisos, musicais, equilibrados e fiéis ao timbre de cada música ou estilo solicitado para guitarra elétrica.
Retorne APENAS um objeto JSON válido (sem blocos markdown ```json).

Utilize os nomes e IDs oficiais do catálogo de fábrica da MK-20:

AMPLIFICADORES OFICIAIS (AMP):
- Marshall JVM410H: "1CL1_410H" (Clean), "2CL2_410H" (Bright Clean), "3CR1_410H" (Light Crunch), "4CR2_410H" (Full Mid Crunch), "5CR3_410H" (Heavy Crunch), "6OD1_410H" (Classic OD), "7OD2_410H" (Modern OD), "8OD3_410H" (Thick Lead), "33OD1_JVMcrunch", "34OD2_JVMcrunch"
- Fender 65 Deluxe Reverb: "13CL1_FenDi" (Classic Blackface), "14CL2_FenDi" (Bright Clean), "15CL3_FenDi" (Loose Vintage)
- Roland JC-120 Jazz Chorus: "25CL1_JC120" (Ultra-clean solid state), "26CL2_JC120" (Chorus clean lendário)
- Marshall JCM800 2203: "51OD1_MS2203" (Brown Sound), "86OD_JCM800" (Classic British Crunch), "87DS_JCM800" (Heavy Metal Distortion)
- Marshall JCM900: "19CL1_J900", "20CL2_J900", "21CL3_J900", "22OD1_J900", "23DS1_J900", "24DS2_J900"
- Mesa Boogie JP-2C: "48CH1_ME" (Mark Crunch), "49CH2_ME" (Lead Solo)
- Mesa Boogie Triple Rectifier: "69OD1_meas", "70OD2_meas", "71CL1_meas", "72CL2_meas", "73CR1_meas"
- ENGL Savage 120: "74OD_ENGL_120" (Speed Metal OD), "75DS_ENGL_120" (Extreme High-Gain DS)
- Soldano SLO-100: "76CL_SoldSolo100" (Hi-End Clean), "77OD_SoldSolo100" (AOR Crunch), "78DS_SoldSolo100" (American High-Gain DS)
- EVH 5150: "79OD_5150III" (EVH Rhythm/Crunch), "80DS_5150III" (EVH Lead High-Gain)
- VOX AC30: "81CL_VOSAC30" (British Class-A Clean), "82OD_VOSAC30" (Top Boost Sweet Breakup)
- Diezel VH4: "83CL_DiezzIVH4" (German Clean), "84OD_DiezzIVH4" (Prog Metal OD), "85DS_DiezzIVH4" (Djent Modern DS)
- Peavey 6505: "88OD_Peavey6505" (Metalcore OD), "89DS_Peavey6505" (Modern High-Gain DS)
- Orange Rockerverb 100: "55CL1_orange"
- Supro 1696RT Black Magick: "54CL1_SUPA"
- Blackstar HT Metal 60: "50DS1_metal"
- Bogner XTC Ecstasy: "64DS1_XC", "65DS2_XC"
- Pro Co RAT (Amp sim): "56OD1_Piranha", "57OD1_RAT", "58OD2_RAT", "59DS1_RAT"
- BOSS Metal Zone (Amp sim): "18HVMetal_1"
- TC Electronic Dark Matter: "12DARK_MATTER1"
- Danelectro Wasabi AX-1: "9OD_AXE1"
- BOSS BD-2 Blues Driver (Amp sim): "10OD1_BLUE", "11OD2_BLUE"
- Horizon Devices: "16HORIZON_1", "17HORIZON_2"
- JHS Loud Is More Good: "27OD1_JHS", "28OD2_JHS", "29OD3_JHS", "30DS_JHS"
- JOYO JF-01: "31OD1_JOYO"
- Boss KTN-MINI-W: "32DS1_JR7W"
- Kohle HM2 Singing Mids: "35DS1_Kohle", "36DS2_Kohle", "37KohleHM"
- M-VAVE Custom: "38DS1_M-VAVE" a "47CL4_M-VAVE"
- NUX Overdrive: "52OD1_NUX", "53OD2_NUX"
- MPF Sounds Room: "60DS1_ROOM", "61DS2_ROOM", "62DS3_ROOM"
- Wangs VT-18: "63CL_WANG"
- YUIMER OD: "66CL1_yuimer", "67CL2_yuimer", "68OD1_yuimer"
- IK Multimedia TONEX: "90DS1_TONE"

GABINETES / IRS OFICIAIS (CAB):
- Guitarra 4x12: "1BogUber412A" (Bogner SM57), "2BogUber412B" (Bogner MD421), "5DizFL412A" (Diezel SM57), "8EngXXL412" (ENGL MD421), "9EvStright412" (EVH U87), "33Mar60A412A" (Marshall 1960A), "36Mar60B412A" (Marshall 1960B U87), "46MbOS412A" (Mesa OS SM57), "57MbTradS412B" (Mesa Traditional MD421), "58OgPPC412A" (Orange PPC412)
- Guitarra 2x12: "10FdTR65R212A" (Fender Twin U87), "11FdTR65R212B" (Fender Twin SM57), "20FdTwRev212A", "22Fman212A" (Friedman SM57), "26HesModn212A" (Hesu Modern), "48MbSD212A" (Mesa 212), "61RJ120azz212" (Roland JC-120), "64VAC30TBC212" (Vox AC30 TopBoost), "66ZillCab212" (Zilla 212)
- Guitarra 1x12 / 1x10: "13FdDelux112A" (Fender Deluxe SM57), "15FdDBface112" (Fender Blackface U87), "16FdDTweed112", "17FdPrinc110A" (Princeton 110), "63VAC15ox112" (Vox AC15)

PEDAIS DE DRIVE / DISTORÇÃO / BOOST (DS / MW):
- Modelos 01 a 12 (Controles Específicos):
  * 01 "MW-808" (Tube Screamer TS-808): "gain" (0), "tone" (50), "level" (100)
  * 02 "MW-GOLD" (Klon Centaur): "gain" (50), "tone" (50), "level" (70)
  * 03 "MW-OCD" (Fulltone OCD): "gain" (0), "tone" (50), "LPtype" (0=off, 1=on), "mode" (1..2, padrão 2), "level" (100)
  * 04 "MW-SD1" (Super Overdrive Boss SD-1): "gain" (0), "tone" (50), "level" (100)
  * 05 "MW-DS1" (Distortion Clássica Boss DS-1): "gain" (0), "tone" (50), "level" (100)
  * 06 "MW-RAT" (ProCo Rat): "gain" (0), "filter" (50), "level" (100)
  * 07 "MW-XEP" (Xotic EP Booster): "gain" (0), "boost" (0=off, 1=on), "bright" (0=off, 1=on)
  * 08 "MW-FACE" (Fuzz Face Vintage): "fuzz" (90), "level" (100)
  * 09 "MW-HRZ" (High-Gain com Gate): "gain" (20), "attack" (1..6, padrão 1), "bright" (100), "gate" (0), "gate mode" (1..2, padrão 1), "level" (100)
  * 10 "MW-EQP" (Overdrive Paramétrico): "gain" (0), "tone" (50), "level" (100), "mode" (1..2, padrão 1)
  * 11 "MW-MUFF" (Big Muff Pi Fuzz): "sustain" (80), "tone" (50), "level" (100)
- Modelos 13 a 52 com Equalização de 8 Parâmetros ("gain": 50, "level": 50, "bass": 50, "middle": 50, "treble": 50, "reso": 0, "pres": 50, "bright": 100):
  * "1._ADAM_RAFOW", "2TS8", "3DS1", "4DS2", "5M-VAVE_OD", "6M-VAVE_DS", "7M-VAVE_TS1", "8M-VAVE_TS2", "9SUPA_1", "10SUPA_2", "11RAT", "12RAT_BT", "13JHS_1", "14JHS_2", "15MT_1", "16MT_2", "17TDS", "18XC_DS", "19QC_DS", "20HIGAIN", "21M-BOOSTER", "22TS-9", "23BIG-DR", "24CL_BOOST", "25BD", "26M90S", "27M2000", "28DS800", "29DS900", "30MAR-DS", "31BOG_DS", "32SONDO", "33MID-BOST", "34RED_DS", "35MODEN_DS", "36SUPEROD", "37BLUES_DR", "38BLACK-BOX", "39BIG-MUFF", "SLASH_USE_YOU"

PRÉ-EFEITOS E DINÂMICA OFICIAIS (FX):
- Compressores: "Compress" (Stompbox), "Compress Pro" (Studio Comp), "F Compress" (Optical Tone Comp)
- Boosters: "Boost" (Clean Boost), "A Boost" (3-Band RC Preamp), "Boost ED" (EP-3 Vintage Pre), "E Boost" (Preamp Boost), "B Boost" (BB Preamp)
- Wahs & Filtros: "Wah-Wah", "Sense-Wah" (Envelope Filter), "Cry-Wah", "Funk-Wah", "Slide-Wah", "X-Wah"
- Pitch & Sintetizadores: "Whammy", "Pitch", "Pitch shifter", "Octave", "Ring", "Lofi"

MODULAÇÃO OFICIAL (MOD):
- "CHORUS", "TRI CHORUS", "FLANGER", "TRI FLANGER", "TREMOLO", "TRI TREMOLO", "OPTO TREMOLO", "PHASER", "VIBRATO", "TRI VIBRATO", "OPTO VIBRATO", "UNIVIBE", "TRI UNIVIBE", "AUTOFILTER", "PHASER STEREO", "FLANGER STEREO", "VIBE STEREO", "CHORUS STEREO", "TREMOLO STEREO", "VIBRATO STEREO"

DELAY OFICIAL (DELAY):
- "Clean", "Echo", "Analog", "PingPong Stereo", "Clean Stereo", "Echo Stereo", "Analog Stereo", "Modern", "Reverse", "Duck", "Dtype", "Lofi", "Tremolo", "Pattern", "Filter", "Dual", "Ice"

REVERB OFICIAL (REVERB):
- "room", "hall", "plate", "room stereo", "hall stereo", "plate stereo", "spring", "spring stereo", "shimmer", "shimmer stereo", "bloom", "cloud", "lofi", "swell"

EQUALIZADOR PÓS-GABINETE (EQ):
- "Guitar EQ 6": "100Hz", "200Hz", "400Hz", "800Hz", "1.6kHz", "3.2kHz"

NOISE SUPPRESSOR (NS):
- "ai gate", "ai ms gate", "soft gate", "hard gate", "pro gate", "compress", "compress pro", "f compress"

DIRETRIZES DE MATRIZES OFICIAIS TRAVADAS + PESQUISA REAL (GOOGLE SEARCH GROUNDING):
1. PESQUISA DO EQUIPAMENTO REAL DA MÚSICA:
   - Identifique o artista, o guitarrista da gravação original, o BPM oficial da música e os equipamentos reais (guitarra, amplificador, overdrive, modulação e delay).
2. SELEÇÃO DA MATRIZ OFICIAL MK-20:
   - Escolha a Matriz Oficial correspondente no campo "style":
     * "Gospel Rock / Melodic Solo" (GOSPEL_MELODIC_SOLO: Solos cantantes, Voz da Verdade, O Escudo, Oficina G3, Solos Melódicos)
     * "Worship / Ambient" (WORSHIP_AMBIENT: Louvor moderno, Hillsong, Elevation, Shimmer & Delay pontuado)
     * "Studio Clean / Pop / Funk" (CLEAN_CRYSTAL: Clean cristalino, Pop, Funk, MPB)
     * "British Rock / Crunch" (CLASSIC_CRUNCH: Classic Rock, Crunch britânico dinâmico)
     * "Texas Blues / Overdrive" (TEXAS_BLUES: Blues, SRV, John Mayer)
     * "Alternative Rock / Post-Grunge" (MODERN_ROCK_ALTERNATIVE: 30 Seconds to Mars, Foo Fighters, Arena Rock)
     * "Epic Lead / Progressive Solo" (EPIC_GILMOUR_LEAD: Pink Floyd, David Gilmour, Sustain progressivo)
     * "Modern High Gain / Metal" (HIGH_GAIN_METAL: Heavy Metal, Thrash, Djent)
3. CALIBRAÇÃO ACÚSTICA OFICIAL DE PALCO E ESTÚDIO:
   - Volume Geral encorpado (Master Volume 64..65, AMP Level 78, CAB Level 84..85).
   - Noise Gate transparente (soft gate thd -52dB em solos/drives; -58dB em clean) que nunca engole o sustain.
   - Brilho aberto e natural: Bright Switch ativo (65..72) e CAB High Cut aberto (7600Hz a 8000Hz).

Formato JSON esperado:
{
  "title": "Nome do Preset",
  "style": "Gospel Rock / Melodic Solo",
  "bpm": 132,
  "youtube_link": "https://...",
  "details": "Explicação técnica do equipamento real usado na gravação e ajustes aplicados na Matriz Oficial",
  "pedals": {
    "FX": { "name": "Pré-Efeito / Dinâmica", "active": true, "model": "Compress", "params": { "Sustain": 4.5, "Attack": 48, "Level": 68, "Blend": 55 } },
    "NS": { "name": "Noise Suppressor", "active": true, "model": "soft gate", "params": { "thd": -52, "inputDetect": 0 } },
    "DS": { "name": "Drive / Boost", "active": true, "model": "MW-808", "params": { "gain": 20, "tone": 56, "level": 90 } },
    "AMP": { "name": "Amplificador", "active": true, "model": "86OD_JCM800", "params": { "Gain": 58, "Level": 78, "Bass": 54, "Middle": 66, "Treble": 66, "Reso": 56, "Pres": 62, "Bright": 68 } },
    "CAB": { "name": "Caixa / IR", "active": true, "model": "33Mar60A412A", "params": { "Level": 85, "Low Cut": 78, "High Cut": 7800 } },
    "EQ": { "name": "Equalizador", "active": true, "model": "Guitar EQ 6", "params": { "100Hz": 1, "200Hz": 0, "400Hz": -1, "800Hz": 2, "1.6kHz": 2, "3.2kHz": 2 } },
    "MOD": { "name": "Modulação", "active": false, "model": "CHORUS", "params": { "speed": 1.0, "depth": 38, "mix": 28, "sync": 0, "syncBpm": 132 } },
    "DELAY": { "name": "Delay", "active": true, "model": "Analog", "params": { "Time": 360, "Fb": 28, "Mix": 22, "Sync": 0, "SyncBPM": 132 } },
    "REVERB": { "name": "Reverb", "active": true, "model": "plate", "params": { "decay": 24, "mix": 18, "highPass" : 80, "lowPass": 80, "modDepth": 10 } }
  }
}
"""

    suspend fun generatePreset(
        songOrStyle: String,
        youtubeUrl: String,
        details: String,
        customApiKey: String?
    ): Result<Mk20Preset> = withContext(Dispatchers.IO) {
        // 1. Verifica primeiro se a música já possui Matriz Master dedicada na biblioteca de referência
        val masterMatch = com.example.data.model.Mk20MasterSongDatabase.findBestMatch("$songOrStyle $youtubeUrl")
        if (masterMatch != null && details.isBlank()) {
            val matched = masterMatch.copy(
                title = if (songOrStyle.isNotBlank()) songOrStyle.trim() else masterMatch.title,
                youtubeLink = if (youtubeUrl.isNotBlank()) youtubeUrl else masterMatch.youtubeLink
            )
            return@withContext Result.success(com.example.util.AcousticStudioGuardrail.sanitize(matched))
        }

        val apiKey = when {
            !customApiKey.isNullOrBlank() -> customApiKey
            BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" -> BuildConfig.GEMINI_API_KEY
            else -> null
        }

        if (apiKey != null) {
            try {
                val selectedMatrix = com.example.data.model.Mk20OfficialToneMatrix.selectBestMatrix(songOrStyle, details)
                val userPrompt = """
Pesquise as especificações reais desta música/artista (BPM oficial da gravação, guitarrista, amplificador e efeitos usados) e gere o preset oficial para a pedaleira M-VAVE MK-20 ancorado na ${selectedMatrix.displayName}:
Música / Estilo: $songOrStyle
Link do YouTube: $youtubeUrl
Detalhes e preferências do timbre: $details

IMPORTANTE:
- Mantenha a espinha dorsal acústica da Matriz Oficial (${selectedMatrix.description}) com volume cheio, brilho aberto e sustain natural.
- Ajuste o BPM real da faixa, o tempo do Delay e os detalhes finos da música.
- O campo 'title' do JSON gerado DEVE ser exatamente: "$songOrStyle".
Retorne SOMENTE o objeto JSON conforme a especificação.
""".trimIndent()

                val response = GeminiClient.generateWithSearchGrounding(
                    apiKey = apiKey,
                    contents = listOf(Content(parts = listOf(Part(text = userPrompt)))),
                    systemInstruction = Content(parts = listOf(Part(text = SYSTEM_INSTRUCTION)))
                )
                val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!rawText.isNullOrBlank()) {
                    val cleanJson = cleanJsonString(rawText)
                    val preset = Mk20Preset.fromJsonString(cleanJson)
                    val titledPreset = if (songOrStyle.isNotBlank()) preset.copy(title = songOrStyle.trim()) else preset
                    // Ancora o preset da IA na Matriz Oficial Travada da MK-20 + Guardião de Estúdio
                    val matrixLocked = com.example.data.model.Mk20OfficialToneMatrix.lockToOfficialMatrix(
                        aiPreset = titledPreset,
                        songOrStyle = songOrStyle,
                        details = details
                    )
                    val sanitized = com.example.util.AcousticStudioGuardrail.sanitize(matrixLocked)
                    return@withContext Result.success(sanitized)
                }
            } catch (e: Exception) {
                // If API call fails, fall back to authentic local engine
            }
        }

        // Intelligent local tone synthesizer based on Official MK-20 Hardware Matrices
        val syntheticPreset = synthesizePresetLocally(songOrStyle, youtubeUrl, details)
        val finalSynthetic = if (songOrStyle.isNotBlank()) syntheticPreset.copy(title = songOrStyle.trim()) else syntheticPreset
        val sanitized = com.example.util.AcousticStudioGuardrail.sanitize(finalSynthetic)
        Result.success(sanitized)
    }

    suspend fun generatePresetFromAudio(
        vibeAnalysis: com.example.util.AudioVibeAnalysis,
        customApiKey: String?
    ): Result<Mk20Preset> = withContext(Dispatchers.IO) {
        val apiKey = when {
            !customApiKey.isNullOrBlank() -> customApiKey
            BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" -> BuildConfig.GEMINI_API_KEY
            else -> null
        }

        if (apiKey != null) {
            try {
                val userPrompt = """
Gere o preset profissional oficial da pedaleira M-VAVE MK-20 analisando esta gravação de áudio e as características acústicas capturadas pelo microfone:
- Estilo detectado: ${vibeAnalysis.detectedStyle}
- Nível de Ganho / Vibe: ${vibeAnalysis.gainVibe}
- BPM Estimado: ${vibeAnalysis.estimatedBpm}
- Energia RMS: ${vibeAnalysis.rmsEnergy}
- Relação de Agudos: ${vibeAnalysis.highFreqRatio}
- Detalhes Acústicos: ${vibeAnalysis.details}
- Título sugerido para o preset: ${vibeAnalysis.title}

IMPORTANTE: O campo 'title' do JSON gerado DEVE ser: "${vibeAnalysis.title}".
Retorne SOMENTE o objeto JSON conforme a especificação do sistema da MK-20, mantendo uma equalização e ambiência musical e balanceada.
""".trimIndent()

                val parts = mutableListOf<Part>()
                if (vibeAnalysis.base64Wav.isNotBlank()) {
                    parts.add(Part(inlineData = InlineData(mimeType = "audio/wav", data = vibeAnalysis.base64Wav)))
                }
                parts.add(Part(text = userPrompt))

                val response = GeminiClient.generateWithSearchGrounding(
                    apiKey = apiKey,
                    contents = listOf(Content(parts = parts)),
                    systemInstruction = Content(parts = listOf(Part(text = SYSTEM_INSTRUCTION)))
                )
                val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!rawText.isNullOrBlank()) {
                    val cleanJson = cleanJsonString(rawText)
                    val preset = Mk20Preset.fromJsonString(cleanJson)
                    val finalPreset = preset.copy(
                        title = vibeAnalysis.title.ifBlank { preset.title }
                    )
                    val matrixLocked = com.example.data.model.Mk20OfficialToneMatrix.lockToOfficialMatrix(
                        aiPreset = finalPreset,
                        songOrStyle = vibeAnalysis.detectedStyle,
                        details = vibeAnalysis.details
                    )
                    val sanitized = com.example.util.AcousticStudioGuardrail.sanitize(matrixLocked)
                    return@withContext Result.success(sanitized)
                }
            } catch (e: Exception) {
                // If API call fails, fall back to high precision acoustic mapping below
            }
        }

        // Direct high-fidelity preset construction from acoustic analysis
        val rawPreset = synthesizeFromAcousticVibe(vibeAnalysis)
        val sanitized = com.example.util.AcousticStudioGuardrail.sanitize(rawPreset)
        Result.success(sanitized)
    }

    private fun synthesizeFromAcousticVibe(vibe: com.example.util.AudioVibeAnalysis): Mk20Preset {
        val title = vibe.title.ifBlank { "MK-20 Mic Tone" }
        val modModel = com.example.data.model.Mk20Catalog.findMod(vibe.suggestedMod)?.name ?: "CHORUS"
        val dlyModel = com.example.data.model.Mk20Catalog.findDelay(vibe.suggestedDelay)?.name ?: "Analog"
        val revModel = com.example.data.model.Mk20Catalog.findReverb(vibe.suggestedReverb)?.name ?: "plate"
        val dsModel = com.example.data.model.Mk20Catalog.findDs(vibe.suggestedDrive)?.name ?: "MW-808"

        return Mk20Preset(
            title = title,
            style = vibe.detectedStyle,
            bpm = vibe.estimatedBpm,
            youtubeLink = "",
            details = vibe.details,
            pedals = Mk20Pedals(
                NS = PedalModule(
                    name = "Noise Suppressor",
                    active = vibe.noiseGateActive,
                    model = if (vibe.noiseGateActive && vibe.detectedStyle.contains("Metal", ignoreCase = true)) "ai ms gate" else "soft gate",
                    params = if (vibe.noiseGateActive && vibe.detectedStyle.contains("Metal", ignoreCase = true)) {
                        mapOf("gate" to 50f, "bias" to 50f, "inputDetect" to 0f)
                    } else {
                        mapOf("thd" to -60f, "inputDetect" to 0f)
                    }
                ),
                DS = PedalModule(
                    name = "Drive / Boost",
                    active = true,
                    model = dsModel,
                    params = com.example.data.model.Mk20Catalog.getDefaultDsParams(dsModel)
                ),
                AMP = PedalModule(
                    name = "Amplificador",
                    active = true,
                    model = vibe.suggestedAmp,
                    params = com.example.data.model.Mk20Catalog.getDefaultAmpParams(vibe.suggestedAmp)
                ),
                CAB = PedalModule(
                    name = "Caixa / IR",
                    active = true,
                    model = vibe.suggestedCab,
                    params = com.example.data.model.Mk20Catalog.getDefaultCabParams(vibe.suggestedCab)
                ),
                MOD = PedalModule(
                    name = "Modulação",
                    active = vibe.suggestedMod != "None",
                    model = modModel,
                    params = com.example.data.model.Mk20Catalog.getDefaultModParams(modModel)
                ),
                DELAY = PedalModule(
                    name = "Delay",
                    active = true,
                    model = dlyModel,
                    params = com.example.data.model.Mk20Catalog.getDefaultDelayParams(dlyModel).toMutableMap().apply {
                        put("Time", (60000f / vibe.estimatedBpm.toFloat() * 0.75f).coerceIn(200f, 500f))
                        put("Mix", 22f)
                        put("Fb", 25f)
                    }
                ),
                REVERB = PedalModule(
                    name = "Reverb",
                    active = true,
                    model = revModel,
                    params = com.example.data.model.Mk20Catalog.getDefaultReverbParams(revModel).toMutableMap().apply {
                        put("mix", 18f)
                        put("decay", 22f)
                    }
                )
            )
        )
    }

    private fun cleanJsonString(raw: String): String {
        return raw.replace("```json", "")
            .replace("```", "")
            .trim()
    }

    fun synthesizePresetLocally(
        songOrStyle: String,
        youtubeUrl: String,
        details: String
    ): Mk20Preset {
        val title = if (songOrStyle.isNotBlank()) songOrStyle.replaceFirstChar { it.uppercase() } else "MK-20 Custom Tone"

        // 1. Direct acoustic match in the Master Song Database of famous tracks
        val masterMatch = com.example.data.model.Mk20MasterSongDatabase.findBestMatch("$songOrStyle $youtubeUrl")
        if (masterMatch != null) {
            val matched = masterMatch.copy(
                title = if (songOrStyle.isNotBlank() && masterMatch.title.equals(songOrStyle.trim(), ignoreCase = true)) masterMatch.title else title,
                youtubeLink = if (youtubeUrl.isNotBlank()) youtubeUrl else masterMatch.youtubeLink
            )
            return com.example.util.AcousticStudioGuardrail.sanitize(matched)
        }

        // 2. Select from the 8 Locked Official M-VAVE MK-20 Hardware Matrices
        val officialMatrix = com.example.data.model.Mk20OfficialToneMatrix.selectBestMatrix(
            songOrStyle = songOrStyle,
            details = details
        )
        val raw = officialMatrix.basePreset.copy(
            title = title,
            youtubeLink = youtubeUrl,
            details = if (details.isNotBlank()) {
                "[${officialMatrix.displayName}] $details — ${officialMatrix.description}"
            } else {
                "[${officialMatrix.displayName}] ${officialMatrix.basePreset.details}"
            }
        )
        return com.example.util.AcousticStudioGuardrail.sanitize(raw)
    }
}
