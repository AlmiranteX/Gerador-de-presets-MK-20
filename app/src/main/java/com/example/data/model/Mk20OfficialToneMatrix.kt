package com.example.data.model

/**
 * Arquitetura de Matrizes Oficiais Travadas (Official Tone Matrices) para a M-VAVE MK-20.
 *
 * Em vez de deixar a IA inventar os 45 parâmetros livremente do zero (o que pode gerar som baixo,
 * abafado ou com microfonia), cada preset parte de uma Matriz Oficial Testada na Física Real da MK-20:
 * - Casamento exato entre Amplificador (AMP) e Caixa/IR (CAB)
 * - Volume Master de Palco (62..68) e Level de Amp/Cab fixos no ponto doce
 * - Noise Gate transparente (-52dB a -58dB) que não engole o sustain
 * - Brilho oficial (Bright Switch ativo e High Cut aberto em 7.5kHz a 8.0kHz)
 *
 * A IA (com Google Search Grounding) atua ajustando a Matriz certa com o BPM real da música,
 * o tempo exato do Delay, a modulação da gravação e um ajuste fino seguro (±8 pontos) na equalização/ganho.
 */
data class OfficialToneMatrix(
    val id: String,
    val displayName: String,
    val description: String,
    val basePreset: Mk20Preset
)

object Mk20OfficialToneMatrix {

    val MATRICES: List<OfficialToneMatrix> = listOf(
        // 1. GOSPEL_MELODIC_SOLO: O Escudo, Voz da Verdade, Solos Cantantes, Rock Melódico
        OfficialToneMatrix(
            id = "GOSPEL_MELODIC_SOLO",
            displayName = "Matriz Oficial: Solo Melódico & Gospel Lead",
            description = "Marshall JCM800 + Caixa 1960A V30 + Tube Screamer empurrando + Compressor suave e Delay de Solo com brilho aberto",
            basePreset = Mk20Preset(
                title = "Gospel Melodic Lead",
                style = "Gospel Rock / Melodic Solo",
                bpm = 128,
                masterVolume = 65,
                details = "Matriz Oficial Solo Cantante: Marshall JCM800 com Bright ativo, empurrado por TS-808, compressor de estúdio para sustain infinito sem microfonia e High Cut aberto em 7.8kHz.",
                pedals = Mk20Pedals(
                    FX = PedalModule("Pré-Efeito / Dinâmica", true, "Compress", mapOf("Sustain" to 4.5f, "Attack" to 48f, "Level" to 68f, "Blend" to 55f)),
                    NS = PedalModule("Noise Suppressor", true, "soft gate", mapOf("thd" to -52f, "inputDetect" to 0f)),
                    DS = PedalModule("Drive / Boost", true, "MW-808", mapOf("gain" to 20f, "tone" to 56f, "level" to 90f)),
                    AMP = PedalModule("Amplificador", true, "86OD_JCM800", mapOf("Gain" to 58f, "Level" to 78f, "Bass" to 54f, "Middle" to 66f, "Treble" to 66f, "Reso" to 56f, "Pres" to 62f, "Bright" to 68f)),
                    CAB = PedalModule("Caixa / IR", true, "33Mar60A412A", mapOf("Level" to 85f, "Low Cut" to 78f, "High Cut" to 7800f)),
                    EQ = PedalModule("Equalizador", true, "Guitar EQ 6", mapOf("100Hz" to 1f, "200Hz" to 0f, "400Hz" to -1f, "800Hz" to 2f, "1.6kHz" to 2f, "3.2kHz" to 2f)),
                    MOD = PedalModule("Modulação", false, "CHORUS", mapOf("speed" to 1.0f, "depth" to 38f, "mix" to 28f, "sync" to 0f, "syncBpm" to 128f)),
                    DELAY = PedalModule("Delay", true, "Analog", mapOf("Time" to 375f, "Fb" to 28f, "Mix" to 22f, "Sync" to 0f, "SyncBPM" to 128f)),
                    REVERB = PedalModule("Reverb", true, "plate", mapOf("decay" to 24f, "mix" to 18f, "highPass" to 80f, "lowPass" to 80f, "modDepth" to 10f))
                )
            )
        ),

        // 2. WORSHIP_AMBIENT: Worship Moderno, Hillsong, Elevation, Bethel
        OfficialToneMatrix(
            id = "WORSHIP_AMBIENT",
            displayName = "Matriz Oficial: Worship & Ambient Chime",
            description = "Roland JC-120 / Vox AC30 + Klon Centaur transparente + Chorus Stereo + Delay Pontuado + Shimmer equilibrado",
            basePreset = Mk20Preset(
                title = "Worship Ambient Official",
                style = "Worship / Ambient",
                bpm = 110,
                masterVolume = 64,
                details = "Matriz Oficial Worship: Timbre cristalino e aberto com Klon Centaur (MW-GOLD), Chorus Estéreo, Delay analógico sincronizado e Shimmer na medida certa.",
                pedals = Mk20Pedals(
                    FX = PedalModule("Pré-Efeito / Dinâmica", true, "Compress", mapOf("Sustain" to 4.8f, "Attack" to 44f, "Level" to 68f, "Blend" to 65f)),
                    NS = PedalModule("Noise Suppressor", true, "soft gate", mapOf("thd" to -58f, "inputDetect" to 0f)),
                    DS = PedalModule("Drive / Boost", true, "MW-GOLD", mapOf("gain" to 20f, "tone" to 54f, "level" to 86f)),
                    AMP = PedalModule("Amplificador", true, "26CL2_JC120", mapOf("Gain" to 32f, "Level" to 78f, "Bass" to 50f, "Middle" to 60f, "Treble" to 66f, "Reso" to 48f, "Pres" to 56f, "Bright" to 72f)),
                    CAB = PedalModule("Caixa / IR", true, "20FdTwRev212A", mapOf("Level" to 84f, "Low Cut" to 75f, "High Cut" to 7800f)),
                    EQ = PedalModule("Equalizador", true, "Guitar EQ 6", mapOf("100Hz" to 0f, "200Hz" to 0f, "400Hz" to -2f, "800Hz" to 1f, "1.6kHz" to 2f, "3.2kHz" to 2f)),
                    MOD = PedalModule("Modulação", true, "CHORUS STEREO", mapOf("speed" to 1.0f, "depth" to 38f, "mode" to 1f, "mix" to 30f, "sync" to 0f, "syncBpm" to 110f)),
                    DELAY = PedalModule("Delay", true, "Analog", mapOf("Time" to 360f, "Fb" to 30f, "Mix" to 22f, "Sync" to 0f, "SyncBPM" to 110f)),
                    REVERB = PedalModule("Reverb", true, "shimmer stereo", mapOf("decay" to 42f, "mix" to 24f, "tone" to 65f, "pitch" to 40f, "amount" to 25f))
                )
            )
        ),

        // 3. CLEAN_CRYSTAL: Clean Cristalino, Pop, Funk, Soul, MPB
        OfficialToneMatrix(
            id = "CLEAN_CRYSTAL",
            displayName = "Matriz Oficial: Studio Clean & Funk",
            description = "Fender 65 Deluxe Bright Clean + Twin 2x12 U87 + Compressor Pro + Brilho cristalino de estúdio",
            basePreset = Mk20Preset(
                title = "Studio Clean Official",
                style = "Studio Clean / Pop / Funk",
                bpm = 115,
                masterVolume = 64,
                details = "Matriz Oficial Clean Cristalino: Fender 65 Deluxe Reverb com caixa Twin 2x12, compressor de estúdio transparente e agudos abertos em 8.0kHz.",
                pedals = Mk20Pedals(
                    FX = PedalModule("Pré-Efeito / Dinâmica", true, "Compress Pro", mapOf("Ratio" to 4.5f, "Gain" to 50f, "Knee" to 30f, "Thd" to 55f, "Attack" to 42f, "Level" to 68f, "Blend" to 70f)),
                    NS = PedalModule("Noise Suppressor", true, "soft gate", mapOf("thd" to -58f, "inputDetect" to 0f)),
                    DS = PedalModule("Drive / Boost", false, "MW-GOLD", mapOf("gain" to 15f, "tone" to 52f, "level" to 85f)),
                    AMP = PedalModule("Amplificador", true, "14CL2_FenDi", mapOf("Gain" to 34f, "Level" to 78f, "Bass" to 52f, "Middle" to 58f, "Treble" to 68f, "Reso" to 48f, "Pres" to 58f, "Bright" to 70f)),
                    CAB = PedalModule("Caixa / IR", true, "10FdTR65R212A", mapOf("Level" to 85f, "Low Cut" to 75f, "High Cut" to 8000f)),
                    EQ = PedalModule("Equalizador", true, "Guitar EQ 6", mapOf("100Hz" to 0f, "200Hz" to -1f, "400Hz" to -2f, "800Hz" to 1f, "1.6kHz" to 2f, "3.2kHz" to 2f)),
                    MOD = PedalModule("Modulação", true, "CHORUS", mapOf("speed" to 1.2f, "depth" to 35f, "mix" to 26f, "sync" to 0f, "syncBpm" to 115f)),
                    DELAY = PedalModule("Delay", false, "Analog", mapOf("Time" to 350f, "Fb" to 22f, "Mix" to 18f, "Sync" to 0f, "SyncBPM" to 115f)),
                    REVERB = PedalModule("Reverb", true, "plate", mapOf("decay" to 22f, "mix" to 18f, "highPass" to 80f, "lowPass" to 80f, "modDepth" to 8f))
                )
            )
        ),

        // 4. CLASSIC_CRUNCH: Classic Rock, British Crunch, Base Dinâmica
        OfficialToneMatrix(
            id = "CLASSIC_CRUNCH",
            displayName = "Matriz Oficial: British Crunch & Rock",
            description = "Marshall JVM410H Crunch + Caixa 1960A 4x12 + Boss SD-1 suave + Presença aberta",
            basePreset = Mk20Preset(
                title = "British Crunch Official",
                style = "British Rock / Crunch",
                bpm = 120,
                masterVolume = 64,
                details = "Matriz Oficial British Crunch: Marshall JVM410H empurrado por Boss SD-1 com ataque definido, médios encorpados e brilho clássico britânico em 7.8kHz.",
                pedals = Mk20Pedals(
                    FX = PedalModule("Pré-Efeito / Dinâmica", true, "Compress", mapOf("Sustain" to 4.2f, "Attack" to 48f, "Level" to 68f, "Blend" to 55f)),
                    NS = PedalModule("Noise Suppressor", true, "soft gate", mapOf("thd" to -52f, "inputDetect" to 0f)),
                    DS = PedalModule("Drive / Boost", true, "MW-SD1", mapOf("gain" to 14f, "tone" to 54f, "level" to 90f)),
                    AMP = PedalModule("Amplificador", true, "4CR2_410H", mapOf("Gain" to 54f, "Level" to 78f, "Bass" to 54f, "Middle" to 64f, "Treble" to 64f, "Reso" to 54f, "Pres" to 60f, "Bright" to 65f)),
                    CAB = PedalModule("Caixa / IR", true, "33Mar60A412A", mapOf("Level" to 84f, "Low Cut" to 78f, "High Cut" to 7800f)),
                    EQ = PedalModule("Equalizador", true, "Guitar EQ 6", mapOf("100Hz" to 1f, "200Hz" to 0f, "400Hz" to -2f, "800Hz" to 1f, "1.6kHz" to 2f, "3.2kHz" to 2f)),
                    MOD = PedalModule("Modulação", false, "CHORUS", mapOf("speed" to 1.0f, "depth" to 40f, "mix" to 30f, "sync" to 0f, "syncBpm" to 120f)),
                    DELAY = PedalModule("Delay", true, "Analog", mapOf("Time" to 350f, "Fb" to 24f, "Mix" to 20f, "Sync" to 0f, "SyncBPM" to 120f)),
                    REVERB = PedalModule("Reverb", true, "plate", mapOf("decay" to 22f, "mix" to 18f, "highPass" to 80f, "lowPass" to 80f, "modDepth" to 10f))
                )
            )
        ),

        // 5. TEXAS_BLUES: Blues, Overdrive Quente, SRV, John Mayer
        OfficialToneMatrix(
            id = "TEXAS_BLUES",
            displayName = "Matriz Oficial: Texas Blues & Vintage Overdrive",
            description = "Fender Deluxe Reverb + Tube Screamer TS-808 + Spring Reverb",
            basePreset = Mk20Preset(
                title = "Texas Blues Official",
                style = "Texas Blues / Overdrive",
                bpm = 112,
                masterVolume = 64,
                details = "Matriz Oficial Texas Blues: Fender 65 Deluxe Reverb empurrado por Tube Screamer TS-808, estalo de palhetada quente e Spring Reverb clássico.",
                pedals = Mk20Pedals(
                    FX = PedalModule("Pré-Efeito / Dinâmica", true, "Compress", mapOf("Sustain" to 4.5f, "Attack" to 48f, "Level" to 68f, "Blend" to 60f)),
                    NS = PedalModule("Noise Suppressor", true, "soft gate", mapOf("thd" to -58f, "inputDetect" to 0f)),
                    DS = PedalModule("Drive / Boost", true, "MW-808", mapOf("gain" to 20f, "tone" to 55f, "level" to 92f)),
                    AMP = PedalModule("Amplificador", true, "13CL1_FenDi", mapOf("Gain" to 50f, "Level" to 78f, "Bass" to 54f, "Middle" to 66f, "Treble" to 65f, "Reso" to 52f, "Pres" to 58f, "Bright" to 65f)),
                    CAB = PedalModule("Caixa / IR", true, "11FdTR65R212B", mapOf("Level" to 84f, "Low Cut" to 75f, "High Cut" to 7600f)),
                    EQ = PedalModule("Equalizador", true, "Guitar EQ 6", mapOf("100Hz" to 1f, "200Hz" to 1f, "400Hz" to -1f, "800Hz" to 2f, "1.6kHz" to 2f, "3.2kHz" to 2f)),
                    MOD = PedalModule("Modulação", false, "OPTO TREMOLO", mapOf("speed" to 1.0f, "depth" to 45f, "level" to 50f, "sync" to 0f, "syncBpm" to 112f)),
                    DELAY = PedalModule("Delay", true, "Echo", mapOf("Time" to 360f, "Fb" to 22f, "Mix" to 18f, "Sync" to 0f, "SyncBPM" to 112f)),
                    REVERB = PedalModule("Reverb", true, "spring", mapOf("decay" to 24f, "mix" to 20f, "highPass" to 80f, "lowPass" to 80f, "combs" to 50f))
                )
            )
        ),

        // 6. MODERN_ROCK_ALTERNATIVE: Alternative Rock, Post-Grunge, Arena Rock
        OfficialToneMatrix(
            id = "MODERN_ROCK_ALTERNATIVE",
            displayName = "Matriz Oficial: Modern Rock & Arena",
            description = "Mesa Boogie Dual Rectifier + Caixa 4x12 Oversized + Booster TS-808 + Plate Reverb",
            basePreset = Mk20Preset(
                title = "Modern Rock Official",
                style = "Alternative Rock / Post-Grunge",
                bpm = 140,
                masterVolume = 64,
                details = "Matriz Oficial Modern Rock: Mesa Boogie Dual Rectifier com graves firmes, médios abertos e definição cristalina em acordes abertos e power chords.",
                pedals = Mk20Pedals(
                    FX = PedalModule("Pré-Efeito / Dinâmica", true, "Compress", mapOf("Sustain" to 4.0f, "Attack" to 48f, "Level" to 68f, "Blend" to 55f)),
                    NS = PedalModule("Noise Suppressor", true, "soft gate", mapOf("thd" to -52f, "inputDetect" to 0f)),
                    DS = PedalModule("Drive / Boost", true, "MW-808", mapOf("gain" to 14f, "tone" to 56f, "level" to 90f)),
                    AMP = PedalModule("Amplificador", true, "70OD2_meas", mapOf("Gain" to 60f, "Level" to 78f, "Bass" to 56f, "Middle" to 62f, "Treble" to 64f, "Reso" to 54f, "Pres" to 60f, "Bright" to 65f)),
                    CAB = PedalModule("Caixa / IR", true, "46MbOS412A", mapOf("Level" to 84f, "Low Cut" to 80f, "High Cut" to 7800f)),
                    EQ = PedalModule("Equalizador", true, "Guitar EQ 6", mapOf("100Hz" to 1f, "200Hz" to -1f, "400Hz" to -2f, "800Hz" to 1f, "1.6kHz" to 2f, "3.2kHz" to 2f)),
                    MOD = PedalModule("Modulação", false, "CHORUS", mapOf("speed" to 1.2f, "depth" to 35f, "mix" to 25f, "sync" to 0f, "syncBpm" to 140f)),
                    DELAY = PedalModule("Delay", true, "Analog", mapOf("Time" to 340f, "Fb" to 22f, "Mix" to 18f, "Sync" to 0f, "SyncBPM" to 140f)),
                    REVERB = PedalModule("Reverb", true, "plate", mapOf("decay" to 22f, "mix" to 18f, "highPass" to 80f, "lowPass" to 80f, "modDepth" to 8f))
                )
            )
        ),

        // 7. EPIC_GILMOUR_LEAD: Solos Épicos, Pink Floyd, Sustain Cremoso
        OfficialToneMatrix(
            id = "EPIC_GILMOUR_LEAD",
            displayName = "Matriz Oficial: Epic Lead & Progressive Solo",
            description = "Marshall JCM800 + Big Muff / Booster + Tape Delay + Hall Reverb",
            basePreset = Mk20Preset(
                title = "Epic Lead Official",
                style = "Epic Lead / Progressive Solo",
                bpm = 125,
                masterVolume = 65,
                details = "Matriz Oficial Epic Lead: Sustentação cremosa e harmônicos abertos inspirados em David Gilmour, com Delay Dtype e Hall Stereo equilibrados.",
                pedals = Mk20Pedals(
                    FX = PedalModule("Pré-Efeito / Dinâmica", true, "Boost ED", mapOf("Gain" to 4.2f, "Grit" to 35f, "Level" to 72f)),
                    NS = PedalModule("Noise Suppressor", true, "soft gate", mapOf("thd" to -52f, "inputDetect" to 0f)),
                    DS = PedalModule("Drive / Boost", true, "MW-MUFF", mapOf("sustain" to 72f, "tone" to 55f, "level" to 90f)),
                    AMP = PedalModule("Amplificador", true, "86OD_JCM800", mapOf("Gain" to 56f, "Level" to 78f, "Bass" to 54f, "Middle" to 66f, "Treble" to 64f, "Reso" to 56f, "Pres" to 60f, "Bright" to 65f)),
                    CAB = PedalModule("Caixa / IR", true, "36Mar60B412A", mapOf("Level" to 84f, "Low Cut" to 80f, "High Cut" to 7600f)),
                    EQ = PedalModule("Equalizador", true, "Guitar EQ 6", mapOf("100Hz" to 1f, "200Hz" to 0f, "400Hz" to -2f, "800Hz" to 2f, "1.6kHz" to 2f, "3.2kHz" to 2f)),
                    MOD = PedalModule("Modulação", true, "FLANGER STEREO", mapOf("speed" to 0.8f, "depth" to 36f, "regen" to 50f, "mix" to 25f, "jet" to 15f, "lfo" to 1f, "sync" to 0f, "syncBpm" to 125f)),
                    DELAY = PedalModule("Delay", true, "Dtype", mapOf("Time" to 440f, "Fb" to 30f, "Mix" to 22f, "Grit" to 20f, "Speed" to 12f, "Depth" to 14f, "Sync" to 0f, "SyncBPM" to 125f)),
                    REVERB = PedalModule("Reverb", true, "hall stereo", mapOf("decay" to 26f, "mix" to 20f, "highPass" to 80f, "lowPass" to 80f, "modDepth" to 10f))
                )
            )
        ),

        // 8. HIGH_GAIN_METAL: Heavy Metal, Thrash, Djent, Hardcore
        OfficialToneMatrix(
            id = "HIGH_GAIN_METAL",
            displayName = "Matriz Oficial: High Gain & Modern Metal",
            description = "ENGL Savage 120 + Mesa Traditional 4x12 + TS-808 Clean Tight Boost + Gate Preciso",
            basePreset = Mk20Preset(
                title = "High Gain Metal Official",
                style = "Modern High Gain / Metal",
                bpm = 135,
                masterVolume = 65,
                details = "Matriz Oficial High Gain: ENGL Savage 120 empurrado por TS-808 Clean Boost, graves secos na palhetada e presença cortante sem apito.",
                pedals = Mk20Pedals(
                    FX = PedalModule("Pré-Efeito / Dinâmica", false, "Compress", mapOf("Sustain" to 4.0f, "Attack" to 50f, "Level" to 68f, "Blend" to 50f)),
                    NS = PedalModule("Noise Suppressor", true, "ai ms gate", mapOf("gate" to 52f, "bias" to 50f, "inputDetect" to 0f)),
                    DS = PedalModule("Drive / Boost", true, "MW-808", mapOf("gain" to 0f, "tone" to 58f, "level" to 95f)),
                    AMP = PedalModule("Amplificador", true, "75DS_ENGL_120", mapOf("Gain" to 66f, "Level" to 78f, "Bass" to 56f, "Middle" to 48f, "Treble" to 64f, "Reso" to 62f, "Pres" to 62f, "Bright" to 65f)),
                    CAB = PedalModule("Caixa / IR", true, "57MbTradS412B", mapOf("Level" to 84f, "Low Cut" to 85f, "High Cut" to 6800f)),
                    EQ = PedalModule("Equalizador", true, "Guitar EQ 6", mapOf("100Hz" to 2f, "200Hz" to -2f, "400Hz" to -3f, "800Hz" to -1f, "1.6kHz" to 2f, "3.2kHz" to 2f)),
                    MOD = PedalModule("Modulação", false, "PHASER", mapOf("speed" to 1.0f, "midcut" to 50f, "reso" to 50f, "fb" to 50f, "sync" to 0f, "syncBpm" to 135f)),
                    DELAY = PedalModule("Delay", true, "Clean", mapOf("Time" to 360f, "Fb" to 22f, "Mix" to 18f, "Sync" to 0f, "SyncBPM" to 135f)),
                    REVERB = PedalModule("Reverb", true, "room", mapOf("decay" to 16f, "mix" to 15f, "highPass" to 85f, "lowPass" to 75f, "modDepth" to 8f))
                )
            )
        )
    )

    /**
     * Identifica a Matriz Oficial ideal com base na música, estilo e detalhes pedidos pelo usuário ou detectados pela IA.
     */
    fun selectBestMatrix(songOrStyle: String, details: String = "", aiStyleHint: String = ""): OfficialToneMatrix {
        val text = "$songOrStyle $details $aiStyleHint".lowercase()

        return when {
            // Solos Gospel, Voz da Verdade, O Escudo, Deus Tremendo, Oficina G3, Solos Melódicos
            text.contains("escudo") || text.contains("voz da verdade") || text.contains("deus tremendo") ||
            text.contains("oficina g3") || text.contains("junqueira") || text.contains("afrânio") ||
            text.contains("afranio") || text.contains("gospel_melodic_solo") ||
            (text.contains("gospel") && (text.contains("solo") || text.contains("lead") || text.contains("drive") || text.contains("rock"))) -> {
                findById("GOSPEL_MELODIC_SOLO")
            }

            // Worship / Ambient / Hillsong / Elevation
            text.contains("worship") || text.contains("ambient") || text.contains("shimmer") ||
            text.contains("elevation") || text.contains("hillsong") || text.contains("bethel") ||
            text.contains("way maker") || text.contains("oceans") || text.contains("gospel") -> {
                findById("WORSHIP_AMBIENT")
            }

            // Metal / Heavy / Thrash / Djent
            text.contains("metal") || text.contains("thrash") || text.contains("djent") ||
            text.contains("metallica") || text.contains("slipknot") || text.contains("megadeth") ||
            text.contains("pantera") || text.contains("iron maiden") || text.contains("high_gain_metal") -> {
                findById("HIGH_GAIN_METAL")
            }

            // Pink Floyd / Gilmour / Solos Progressivos
            text.contains("pink floyd") || text.contains("gilmour") || text.contains("numb") ||
            text.contains("epic_gilmour_lead") || text.contains("fuzz") -> {
                findById("EPIC_GILMOUR_LEAD")
            }

            // Solo Melódico Geral
            text.contains("solo") || text.contains("lead") || text.contains("santana") ||
            text.contains("gary moore") || text.contains("slash") || text.contains("sweet child") -> {
                findById("GOSPEL_MELODIC_SOLO")
            }

            // Blues / SRV / Mayer
            text.contains("blues") || text.contains("srv") || text.contains("mayer") ||
            text.contains("bb king") || text.contains("texas_blues") -> {
                findById("TEXAS_BLUES")
            }

            // Alternative Rock / 30 Seconds to Mars / Foo Fighters / Post-Grunge
            text.contains("30 seconds") || text.contains("thirty seconds") || text.contains("the kill") ||
            text.contains("alternative") || text.contains("grunge") || text.contains("foo fighters") ||
            text.contains("linkin park") || text.contains("modern_rock_alternative") -> {
                findById("MODERN_ROCK_ALTERNATIVE")
            }

            // Clean / Pop / Funk / MPB
            text.contains("clean") || text.contains("limpo") || text.contains("pop") ||
            text.contains("funk") || text.contains("mpb") || text.contains("dedilhado") ||
            text.contains("clean_crystal") -> {
                findById("CLEAN_CRYSTAL")
            }

            // Classic Rock / Crunch / Default
            else -> findById("CLASSIC_CRUNCH")
        }
    }

    fun findById(id: String): OfficialToneMatrix {
        return MATRICES.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: MATRICES[3] // CLASSIC_CRUNCH fallback
    }

    /**
     * Ancora o preset gerado pela IA na espinha dorsal da Matriz Oficial selecionada,
     * permitindo apenas ajustes musicais seguros (±8 pontos no Amp/Drive, BPM real e tempo de Delay),
     * garantindo 100% de volume oficial, brilho aberto e zero microfonia.
     */
    fun lockToOfficialMatrix(
        aiPreset: Mk20Preset,
        songOrStyle: String,
        details: String = ""
    ): Mk20Preset {
        val matrix = selectBestMatrix(songOrStyle, details, "${aiPreset.style} ${aiPreset.details}")
        val base = matrix.basePreset
        val basePedals = base.pedals
        val aiPedals = aiPreset.pedals

        val validBpm = aiPreset.bpm.coerceIn(60, 215)

        // Permite à IA refinar sutilmente (±8 pontos) o Amp da Matriz sem quebrar a estrutura de fábrica
        val baseAmpParams = basePedals.AMP?.params ?: emptyMap()
        val aiAmpParams = aiPedals.AMP?.params ?: emptyMap()
        val lockedAmpParams = baseAmpParams.toMutableMap().apply {
            for (key in listOf("Gain", "Bass", "Middle", "Treble", "Pres", "Reso")) {
                val baseVal = baseAmpParams[key] ?: 55f
                val aiVal = aiAmpParams[key] ?: baseVal
                // Trava a variação máxima em ±6 pontos ao redor do ponto doce da Matriz Oficial
                put(key, aiVal.coerceIn((baseVal - 6f).coerceAtLeast(25f), (baseVal + 6f).coerceAtMost(80f)))
            }
            // Garante Level e Bright oficiais da Matriz para o som nunca ficar baixo nem sem brilho
            put("Level", baseAmpParams["Level"] ?: 78f)
            put("Bright", (aiAmpParams["Bright"] ?: (baseAmpParams["Bright"] ?: 65f)).coerceIn(60f, 78f))
        }

        // Gabinete (CAB) travado no casamento oficial da Matriz (Level 84-85 e High Cut aberto)
        val baseCabParams = basePedals.CAB?.params ?: emptyMap()
        val lockedCabParams = baseCabParams.toMutableMap()

        // Drive (DS) ancorado na Matriz com leve ajuste fino de ganho (±6 pontos)
        val baseDsParams = basePedals.DS?.params ?: emptyMap()
        val aiDsParams = aiPedals.DS?.params ?: emptyMap()
        val lockedDsParams = baseDsParams.toMutableMap().apply {
            if (baseDsParams.containsKey("gain")) {
                val bGain = baseDsParams["gain"] ?: 15f
                val aGain = aiDsParams["gain"] ?: aiDsParams["Gain"] ?: bGain
                put("gain", aGain.coerceIn((bGain - 6f).coerceAtLeast(0f), (bGain + 6f).coerceAtMost(35f)))
            }
            if (baseDsParams.containsKey("tone")) {
                val bTone = baseDsParams["tone"] ?: 54f
                val aTone = aiDsParams["tone"] ?: aiDsParams["Tone"] ?: bTone
                put("tone", aTone.coerceIn(48f, 62f))
            }
            if (baseDsParams.containsKey("level")) {
                put("level", baseDsParams["level"] ?: 90f)
            }
        }

        // Delay sincronizado com o BPM real da música pesquisada pela IA
        val baseDlyParams = basePedals.DELAY?.params ?: emptyMap()
        val aiDlyParams = aiPedals.DELAY?.params ?: emptyMap()
        val dottedEighthMs = (60000f / validBpm.toFloat() * 0.75f).coerceIn(220f, 480f)
        val lockedDlyParams = baseDlyParams.toMutableMap().apply {
            val aiTime = aiDlyParams["Time"] ?: dottedEighthMs
            put("Time", aiTime.coerceIn(220f, 480f))
            put("SyncBPM", validBpm.toFloat())
        }

        // Modulação: respeita se a IA identificou modulação específica na música, com mix equilibrado
        val baseMod = basePedals.MOD
        val aiMod = aiPedals.MOD
        val lockedMod = if (aiMod != null && aiMod.active) {
            val validModModel = Mk20Catalog.findMod(aiMod.model)?.name ?: baseMod?.model ?: "CHORUS"
            val modParams = Mk20Catalog.getDefaultModParams(validModModel).toMutableMap().apply {
                putAll(aiMod.params)
                if (containsKey("mix")) put("mix", (this["mix"] ?: 28f).coerceIn(18f, 34f))
                if (containsKey("syncBpm")) put("syncBpm", validBpm.toFloat())
            }
            baseMod?.copy(active = true, model = validModModel, params = modParams)
        } else {
            baseMod
        }

        val lockedPedals = basePedals.copy(
            FX = basePedals.FX,
            NS = basePedals.NS,
            DS = basePedals.DS?.copy(params = lockedDsParams),
            AMP = basePedals.AMP?.copy(params = lockedAmpParams),
            CAB = basePedals.CAB?.copy(params = lockedCabParams),
            EQ = basePedals.EQ,
            MOD = lockedMod,
            DELAY = basePedals.DELAY?.copy(params = lockedDlyParams),
            REVERB = basePedals.REVERB
        )

        val enrichedDetails = if (aiPreset.details.isNotBlank()) {
            "[${matrix.displayName}] ${aiPreset.details}"
        } else {
            matrix.basePreset.details
        }

        return base.copy(
            title = aiPreset.title.ifBlank { base.title },
            style = aiPreset.style.ifBlank { base.style },
            bpm = validBpm,
            masterVolume = base.masterVolume.coerceIn(62, 68),
            youtubeLink = aiPreset.youtubeLink,
            details = enrichedDetails,
            pedals = lockedPedals
        )
    }
}
