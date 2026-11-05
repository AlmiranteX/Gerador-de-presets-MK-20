package com.example.data.model

data class MasterSongItem(
    val id: String,
    val title: String,
    val artist: String,
    val style: String,
    val bpm: Int,
    val realRigDescription: String,
    val preset: Mk20Preset
)

object Mk20MasterSongDatabase {

    val SONGS: List<MasterSongItem> = listOf(
        MasterSongItem(
            id = "sweet_child_o_mine",
            title = "Sweet Child O' Mine",
            artist = "Guns N' Roses (Slash)",
            style = "Hard Rock",
            bpm = 125,
            realRigDescription = "Gibson Les Paul 1959 (Braço) -> Marshall Silver Jubilee 2555 + Celestion V30 + TS-808 Clean Boost",
            preset = Mk20Preset(
                title = "Sweet Child O' Mine",
                style = "Hard Rock / Solo",
                bpm = 125,
                youtubeLink = "https://www.youtube.com/watch?v=1w7OgIMMRc4",
                details = "Timbre lendário de Slash: Marshall JCM800 empurrado por TS-808 com ganho baixo e level alto para focar os médios. Caixa 4x12 com Low Cut em 80Hz e High Cut em 6.8kHz eliminando qualquer abelha digital.",
                pedals = Mk20Pedals(
                    FX = PedalModule("Pré-Efeito / Dinâmica", true, "Boost", mapOf("Gain" to 2.5f, "Level" to 75f)),
                    NS = PedalModule("Noise Suppressor", true, "soft gate", mapOf("thd" to -55f, "inputDetect" to 0f)),
                    DS = PedalModule("Drive / Boost", true, "MW-808", mapOf("gain" to 8f, "tone" to 52f, "level" to 95f)),
                    AMP = PedalModule("Amplificador", true, "51OD1_MS2203", mapOf("Gain" to 62f, "Level" to 76f, "Bass" to 58f, "Middle" to 65f, "Treble" to 60f, "Reso" to 52f, "Pres" to 55f, "Bright" to 50f)),
                    CAB = PedalModule("Caixa / IR", true, "33Mar60A412A", mapOf("Level" to 82f, "Low Cut" to 80f, "High Cut" to 6800f)),
                    EQ = PedalModule("Equalizador", true, "Guitar EQ 6", mapOf("100Hz" to 1f, "200Hz" to -1f, "400Hz" to -2f, "800Hz" to 1f, "1.6kHz" to 3f, "3.2kHz" to 2f)),
                    MOD = PedalModule("Modulação", false, "CHORUS", mapOf("speed" to 1.0f, "depth" to 40f, "mix" to 35f, "sync" to 0f, "syncBpm" to 125f)),
                    DELAY = PedalModule("Delay", true, "Analog", mapOf("Time" to 340f, "Fb" to 28f, "Mix" to 22f, "Sync" to 0f, "SyncBPM" to 125f)),
                    REVERB = PedalModule("Reverb", true, "plate", mapOf("decay" to 24f, "mix" to 18f, "highPass" to 80f, "lowPass" to 80f, "modDepth" to 10f))
                )
            )
        ),
        MasterSongItem(
            id = "master_of_puppets",
            title = "Master of Puppets",
            artist = "Metallica (James Hetfield)",
            style = "Thrash Metal",
            bpm = 212,
            realRigDescription = "ESP Explorer -> Mesa Boogie Mark IIC++ (EQ em V) + Mesa OS 4x12 (V30) + TS-9 Boost + Gate Rápido",
            preset = Mk20Preset(
                title = "Master of Puppets",
                style = "Thrash Metal / Riff",
                bpm = 212,
                youtubeLink = "https://www.youtube.com/watch?v=K6LA7v1PApU",
                details = "O Santo Graal do som de base de metal: Mesa Boogie Mark Series com médios cavados (V-shape), graves apertados cortando em 90Hz e agudo cirúrgico em 6.5kHz com Noise Gate staccato.",
                pedals = Mk20Pedals(
                    FX = PedalModule("Pré-Efeito / Dinâmica", false, "Compress", mapOf("Sustain" to 4.0f, "Attack" to 45f, "Level" to 70f, "Blend" to 50f)),
                    NS = PedalModule("Noise Suppressor", true, "ai ms gate", mapOf("gate" to 56f, "bias" to 50f, "inputDetect" to 0f)),
                    DS = PedalModule("Drive / Boost", true, "MW-808", mapOf("gain" to 0f, "tone" to 60f, "level" to 100f)),
                    AMP = PedalModule("Amplificador", true, "48CH1_ME", mapOf("Gain" to 72f, "Level" to 74f, "Bass" to 60f, "Middle" to 34f, "Treble" to 70f, "Reso" to 66f, "Pres" to 65f, "Bright" to 0f)),
                    CAB = PedalModule("Caixa / IR", true, "46MbOS412A", mapOf("Level" to 84f, "Low Cut" to 90f, "High Cut" to 6500f)),
                    EQ = PedalModule("Equalizador", true, "Guitar EQ 6", mapOf("100Hz" to 3f, "200Hz" to -2f, "400Hz" to -4f, "800Hz" to -3f, "1.6kHz" to 3f, "3.2kHz" to 4f)),
                    MOD = PedalModule("Modulação", false, "PHASER", mapOf("speed" to 1.0f, "midcut" to 50f, "reso" to 50f, "fb" to 50f, "sync" to 0f, "syncBpm" to 212f)),
                    DELAY = PedalModule("Delay", false, "Clean", mapOf("Time" to 280f, "Fb" to 20f, "Mix" to 18f, "Sync" to 0f, "SyncBPM" to 212f)),
                    REVERB = PedalModule("Reverb", true, "room", mapOf("decay" to 14f, "mix" to 14f, "highPass" to 85f, "lowPass" to 75f, "modDepth" to 8f))
                )
            )
        ),
        MasterSongItem(
            id = "back_in_black",
            title = "Back in Black",
            artist = "AC/DC (Angus Young)",
            style = "Classic Rock",
            bpm = 130,
            realRigDescription = "Gibson SG 1968 -> Marshall JTM45 / Super Lead 100 cranked + Celestion Greenback + Room Reverb",
            preset = Mk20Preset(
                title = "Back in Black",
                style = "Classic Rock / Riff",
                bpm = 130,
                youtubeLink = "https://www.youtube.com/watch?v=pAgnJDJN4VA",
                details = "Puro crunch valvulado britânico sem ganho exagerado. Médios crocantes, zero delay e apenas o reverb de sala do Compass Point Studios.",
                pedals = Mk20Pedals(
                    FX = PedalModule("Pré-Efeito / Dinâmica", false, "Compress", mapOf("Sustain" to 3.0f, "Attack" to 50f, "Level" to 70f, "Blend" to 50f)),
                    NS = PedalModule("Noise Suppressor", false, "soft gate", mapOf("thd" to -60f, "inputDetect" to 0f)),
                    DS = PedalModule("Drive / Boost", false, "MW-SD1", mapOf("gain" to 0f, "tone" to 50f, "level" to 100f)),
                    AMP = PedalModule("Amplificador", true, "86OD_JCM800", mapOf("Gain" to 52f, "Level" to 78f, "Bass" to 54f, "Middle" to 68f, "Treble" to 62f, "Reso" to 52f, "Pres" to 58f, "Bright" to 60f)),
                    CAB = PedalModule("Caixa / IR", true, "33Mar60A412A", mapOf("Level" to 84f, "Low Cut" to 80f, "High Cut" to 7200f)),
                    EQ = PedalModule("Equalizador", true, "Guitar EQ 6", mapOf("100Hz" to 0f, "200Hz" to 0f, "400Hz" to -1f, "800Hz" to 2f, "1.6kHz" to 3f, "3.2kHz" to 1f)),
                    MOD = PedalModule("Modulação", false, "CHORUS", mapOf("speed" to 1.0f, "depth" to 50f, "mix" to 50f, "sync" to 0f, "syncBpm" to 130f)),
                    DELAY = PedalModule("Delay", false, "Analog", mapOf("Time" to 350f, "Fb" to 20f, "Mix" to 15f, "Sync" to 0f, "SyncBPM" to 130f)),
                    REVERB = PedalModule("Reverb", true, "room", mapOf("decay" to 18f, "mix" to 16f, "highPass" to 80f, "lowPass" to 80f, "modDepth" to 8f))
                )
            )
        ),
        MasterSongItem(
            id = "comfortably_numb",
            title = "Comfortably Numb (Solo)",
            artist = "Pink Floyd (David Gilmour)",
            style = "Progressive Rock",
            bpm = 127,
            realRigDescription = "Fender Black Strat -> Electro-Harmonix Big Muff Ram's Head + Hiwatt DR103 + Binson Echorec + Mistress Flanger",
            preset = Mk20Preset(
                title = "Comfortably Numb (Solo)",
                style = "Progressive Rock / Solo",
                bpm = 127,
                youtubeLink = "https://www.youtube.com/watch?v=_FrOQC-zEog",
                details = "O solo mais aclamado da história: Big Muff Pi fornecendo sustain infinito sobre um pré limpo encorpado, tape delay longo de 470ms e flanger estéreo aberto.",
                pedals = Mk20Pedals(
                    FX = PedalModule("Pré-Efeito / Dinâmica", true, "Boost ED", mapOf("Gain" to 4.2f, "Grit" to 35f, "Level" to 75f)),
                    NS = PedalModule("Noise Suppressor", true, "soft gate", mapOf("thd" to -55f, "inputDetect" to 0f)),
                    DS = PedalModule("Drive / Boost", true, "MW-MUFF", mapOf("sustain" to 82f, "tone" to 52f, "level" to 90f)),
                    AMP = PedalModule("Amplificador", true, "76CL_SoldSolo100", mapOf("Gain" to 38f, "Level" to 76f, "Bass" to 56f, "Middle" to 62f, "Treble" to 64f, "Reso" to 55f, "Pres" to 60f, "Bright" to 45f)),
                    CAB = PedalModule("Caixa / IR", true, "36Mar60B412A", mapOf("Level" to 82f, "Low Cut" to 85f, "High Cut" to 7000f)),
                    EQ = PedalModule("Equalizador", true, "Guitar EQ 6", mapOf("100Hz" to 1f, "200Hz" to 0f, "400Hz" to -2f, "800Hz" to 1f, "1.6kHz" to 3f, "3.2kHz" to 2f)),
                    MOD = PedalModule("Modulação", true, "FLANGER STEREO", mapOf("speed" to 0.8f, "depth" to 45f, "regen" to 60f, "mix" to 30f, "jet" to 15f, "lfo" to 1f, "sync" to 0f, "syncBpm" to 127f)),
                    DELAY = PedalModule("Delay", true, "Dtype", mapOf("Time" to 470f, "Fb" to 42f, "Mix" to 30f, "Grit" to 25f, "Speed" to 12f, "Depth" to 14f, "Sync" to 0f, "SyncBPM" to 127f)),
                    REVERB = PedalModule("Reverb", true, "hall stereo", mapOf("decay" to 45f, "mix" to 22f, "highPass" to 80f, "lowPass" to 80f, "modDepth" to 10f))
                )
            )
        ),
        MasterSongItem(
            id = "sultans_of_swing",
            title = "Sultans of Swing",
            artist = "Dire Straits (Mark Knopfler)",
            style = "Classic Clean",
            bpm = 148,
            realRigDescription = "Fender Stratocaster 1961 (Posição 2 Ponte+Meio) -> Fender Twin Reverb 1965 + Dan Armstrong Orange Squeezer",
            preset = Mk20Preset(
                title = "Sultans of Swing",
                style = "Classic Clean / Fingerstyle",
                bpm = 148,
                youtubeLink = "https://www.youtube.com/watch?v=0fAQhSRLQnM",
                details = "O estalo cristalino de Mark Knopfler: Fender Twin limpo com compressão rápida que nivela a dinâmica das dedilhadas, médios presentes e Spring Reverb curto.",
                pedals = Mk20Pedals(
                    FX = PedalModule("Pré-Efeito / Dinâmica", true, "F Compress", mapOf("Ratio" to 4.5f, "Gain" to 50f, "Knee" to 40f, "Thd" to 50f, "Attack" to 35f, "Tone" to 65f, "Level" to 75f, "Blend" to 80f)),
                    NS = PedalModule("Noise Suppressor", true, "soft gate", mapOf("thd" to -65f, "inputDetect" to 0f)),
                    DS = PedalModule("Drive / Boost", false, "MW-808", mapOf("gain" to 0f, "tone" to 50f, "level" to 100f)),
                    AMP = PedalModule("Amplificador", true, "14CL2_FenDi", mapOf("Gain" to 32f, "Level" to 78f, "Bass" to 48f, "Middle" to 55f, "Treble" to 72f, "Reso" to 45f, "Pres" to 60f, "Bright" to 85f)),
                    CAB = PedalModule("Caixa / IR", true, "10FdTR65R212A", mapOf("Level" to 82f, "Low Cut" to 75f, "High Cut" to 7800f)),
                    EQ = PedalModule("Equalizador", true, "Guitar EQ 6", mapOf("100Hz" to -1f, "200Hz" to 0f, "400Hz" to -2f, "800Hz" to 1f, "1.6kHz" to 3f, "3.2kHz" to 3f)),
                    MOD = PedalModule("Modulação", false, "CHORUS", mapOf("speed" to 1.0f, "depth" to 50f, "mix" to 50f, "sync" to 0f, "syncBpm" to 148f)),
                    DELAY = PedalModule("Delay", false, "Analog", mapOf("Time" to 200f, "Fb" to 15f, "Mix" to 15f, "Sync" to 0f, "SyncBPM" to 148f)),
                    REVERB = PedalModule("Reverb", true, "spring", mapOf("decay" to 28f, "mix" to 22f, "highPass" to 80f, "lowPass" to 80f, "combs" to 50f))
                )
            )
        ),
        MasterSongItem(
            id = "pride_and_joy",
            title = "Pride and Joy",
            artist = "Stevie Ray Vaughan",
            style = "Texas Blues",
            bpm = 124,
            realRigDescription = "Fender 'Number One' Strat -> Ibanez TS-808 (Ganho 2, Level 10) -> Fender Super Reverb / Vibroverb 1964",
            preset = Mk20Preset(
                title = "Pride and Joy",
                style = "Texas Blues / Overdrive",
                bpm = 124,
                youtubeLink = "https://www.youtube.com/watch?v=0vo23H9J8LE",
                details = "O ataque explosivo de SRV: TS-808 empurrando amplificador Fender Blackface até o limiar de saturação, com graves quentes e agudo gordo sem estridência.",
                pedals = Mk20Pedals(
                    FX = PedalModule("Pré-Efeito / Dinâmica", true, "Compress", mapOf("Sustain" to 4.5f, "Attack" to 48f, "Level" to 72f, "Blend" to 60f)),
                    NS = PedalModule("Noise Suppressor", false, "soft gate", mapOf("thd" to -60f, "inputDetect" to 0f)),
                    DS = PedalModule("Drive / Boost", true, "MW-808", mapOf("gain" to 22f, "tone" to 55f, "level" to 100f)),
                    AMP = PedalModule("Amplificador", true, "13CL1_FenDi", mapOf("Gain" to 55f, "Level" to 75f, "Bass" to 58f, "Middle" to 66f, "Treble" to 66f, "Reso" to 52f, "Pres" to 58f, "Bright" to 60f)),
                    CAB = PedalModule("Caixa / IR", true, "11FdTR65R212B", mapOf("Level" to 82f, "Low Cut" to 75f, "High Cut" to 7200f)),
                    EQ = PedalModule("Equalizador", true, "Guitar EQ 6", mapOf("100Hz" to 1f, "200Hz" to 1f, "400Hz" to -1f, "800Hz" to 2f, "1.6kHz" to 3f, "3.2kHz" to 2f)),
                    MOD = PedalModule("Modulação", false, "OPTO TREMOLO", mapOf("speed" to 1.0f, "depth" to 50f, "level" to 50f, "sync" to 0f, "syncBpm" to 124f)),
                    DELAY = PedalModule("Delay", false, "Analog", mapOf("Time" to 320f, "Fb" to 20f, "Mix" to 15f, "Sync" to 0f, "SyncBPM" to 124f)),
                    REVERB = PedalModule("Reverb", true, "spring", mapOf("decay" to 26f, "mix" to 22f, "highPass" to 80f, "lowPass" to 80f, "combs" to 50f))
                )
            )
        ),
        MasterSongItem(
            id = "the_trooper",
            title = "The Trooper",
            artist = "Iron Maiden (Murray / Smith)",
            style = "Heavy Metal",
            bpm = 160,
            realRigDescription = "Fender Strat com humbucker / Gibson Les Paul -> Marshall JCM800 2203 + Boss SD-1 Boost",
            preset = Mk20Preset(
                title = "The Trooper",
                style = "Heavy Metal / Lead",
                bpm = 160,
                youtubeLink = "https://www.youtube.com/watch?v=X4bgXH3sJ2Q",
                details = "O ataque clássico da NWOBHM: Marshall JCM800 com médios destacados no talo (Middle 70%), SD-1 empurrando para sustain longo e caixa 4x12 britânica.",
                pedals = Mk20Pedals(
                    FX = PedalModule("Pré-Efeito / Dinâmica", false, "Compress", mapOf("Sustain" to 4.0f, "Attack" to 50f, "Level" to 70f, "Blend" to 50f)),
                    NS = PedalModule("Noise Suppressor", true, "soft gate", mapOf("thd" to -58f, "inputDetect" to 0f)),
                    DS = PedalModule("Drive / Boost", true, "MW-SD1", mapOf("gain" to 10f, "tone" to 55f, "level" to 90f)),
                    AMP = PedalModule("Amplificador", true, "86OD_JCM800", mapOf("Gain" to 66f, "Level" to 76f, "Bass" to 52f, "Middle" to 70f, "Treble" to 65f, "Reso" to 56f, "Pres" to 68f, "Bright" to 50f)),
                    CAB = PedalModule("Caixa / IR", true, "33Mar60A412A", mapOf("Level" to 84f, "Low Cut" to 85f, "High Cut" to 6800f)),
                    EQ = PedalModule("Equalizador", true, "Guitar EQ 6", mapOf("100Hz" to 0f, "200Hz" to -1f, "400Hz" to -2f, "800Hz" to 2f, "1.6kHz" to 3f, "3.2kHz" to 2f)),
                    MOD = PedalModule("Modulação", false, "CHORUS", mapOf("speed" to 1.0f, "depth" to 40f, "mix" to 30f, "sync" to 0f, "syncBpm" to 160f)),
                    DELAY = PedalModule("Delay", true, "Clean", mapOf("Time" to 375f, "Fb" to 24f, "Mix" to 18f, "Sync" to 0f, "SyncBPM" to 160f)),
                    REVERB = PedalModule("Reverb", true, "room", mapOf("decay" to 18f, "mix" to 16f, "highPass" to 80f, "lowPass" to 80f, "modDepth" to 8f))
                )
            )
        ),
        MasterSongItem(
            id = "smells_like_teen_spirit",
            title = "Smells Like Teen Spirit",
            artist = "Nirvana (Kurt Cobain)",
            style = "Grunge",
            bpm = 117,
            realRigDescription = "Fender Jaguar / Mustang -> Boss DS-1 / SansAmp -> Mesa Boogie Studio .22 / Marshall JCM900",
            preset = Mk20Preset(
                title = "Smells Like Teen Spirit",
                style = "Grunge / 90s Alternative",
                bpm = 117,
                youtubeLink = "https://www.youtube.com/watch?v=hTWKbfoikeg",
                details = "O contraste dos anos 90: Small Clone Chorus no clean do verso e Boss DS-1 explosivo rasgando o refrão com Marshall saturado.",
                pedals = Mk20Pedals(
                    FX = PedalModule("Pré-Efeito / Dinâmica", false, "Compress", mapOf("Sustain" to 4.0f, "Attack" to 50f, "Level" to 70f, "Blend" to 50f)),
                    NS = PedalModule("Noise Suppressor", true, "soft gate", mapOf("thd" to -55f, "inputDetect" to 0f)),
                    DS = PedalModule("Drive / Boost", true, "MW-DS1", mapOf("gain" to 75f, "tone" to 48f, "level" to 85f)),
                    AMP = PedalModule("Amplificador", true, "19CL1_J900", mapOf("Gain" to 45f, "Level" to 76f, "Bass" to 58f, "Middle" to 50f, "Treble" to 68f, "Reso" to 55f, "Pres" to 60f, "Bright" to 50f)),
                    CAB = PedalModule("Caixa / IR", true, "33Mar60A412A", mapOf("Level" to 82f, "Low Cut" to 80f, "High Cut" to 7000f)),
                    EQ = PedalModule("Equalizador", true, "Guitar EQ 6", mapOf("100Hz" to 1f, "200Hz" to 0f, "400Hz" to -2f, "800Hz" to 1f, "1.6kHz" to 3f, "3.2kHz" to 2f)),
                    MOD = PedalModule("Modulação", true, "CHORUS", mapOf("speed" to 1.2f, "depth" to 55f, "mix" to 40f, "sync" to 0f, "syncBpm" to 117f)),
                    DELAY = PedalModule("Delay", false, "Analog", mapOf("Time" to 320f, "Fb" to 25f, "Mix" to 20f, "Sync" to 0f, "SyncBPM" to 117f)),
                    REVERB = PedalModule("Reverb", true, "room", mapOf("decay" to 22f, "mix" to 18f, "highPass" to 80f, "lowPass" to 80f, "modDepth" to 10f))
                )
            )
        ),
        MasterSongItem(
            id = "enter_sandman",
            title = "Enter Sandman",
            artist = "Metallica (Kirk Hammett)",
            style = "Metal Lead",
            bpm = 123,
            realRigDescription = "ESP Kirk Hammett Signature -> Mesa Boogie Quad Preamp / Mark IV -> Mesa 4x12 Celestion V30 + Cry Baby Wah",
            preset = Mk20Preset(
                title = "Enter Sandman (Lead)",
                style = "Metal / Solo & Riff",
                bpm = 123,
                youtubeLink = "https://www.youtube.com/watch?v=CD-E-LDc384",
                details = "O timbre com mais punch do Black Album: Mesa Boogie Lead com Cry Baby disponível no FX, TS-808 apertando o ataque e Delay estéreo amplo.",
                pedals = Mk20Pedals(
                    FX = PedalModule("Pré-Efeito / Dinâmica", false, "Cry-Wah", mapOf("Value" to 50f, "Gain" to 50f, "Level" to 80f)),
                    NS = PedalModule("Noise Suppressor", true, "ai ms gate", mapOf("gate" to 54f, "bias" to 50f, "inputDetect" to 0f)),
                    DS = PedalModule("Drive / Boost", true, "MW-808", mapOf("gain" to 0f, "tone" to 60f, "level" to 100f)),
                    AMP = PedalModule("Amplificador", true, "49CH2_ME", mapOf("Gain" to 74f, "Level" to 74f, "Bass" to 56f, "Middle" to 45f, "Treble" to 68f, "Reso" to 62f, "Pres" to 66f, "Bright" to 0f)),
                    CAB = PedalModule("Caixa / IR", true, "46MbOS412A", mapOf("Level" to 84f, "Low Cut" to 85f, "High Cut" to 6500f)),
                    EQ = PedalModule("Equalizador", true, "Guitar EQ 6", mapOf("100Hz" to 2f, "200Hz" to -1f, "400Hz" to -3f, "800Hz" to -1f, "1.6kHz" to 3f, "3.2kHz" to 4f)),
                    MOD = PedalModule("Modulação", false, "PHASER", mapOf("speed" to 1.0f, "midcut" to 50f, "reso" to 50f, "fb" to 50f, "sync" to 0f, "syncBpm" to 123f)),
                    DELAY = PedalModule("Delay", true, "Analog", mapOf("Time" to 485f, "Fb" to 32f, "Mix" to 24f, "Sync" to 0f, "SyncBPM" to 123f)),
                    REVERB = PedalModule("Reverb", true, "hall", mapOf("decay" to 32f, "mix" to 20f, "highPass" to 80f, "lowPass" to 80f, "modDepth" to 10f))
                )
            )
        ),
        MasterSongItem(
            id = "gravity_mayer",
            title = "Gravity",
            artist = "John Mayer",
            style = "Modern Blues",
            bpm = 90,
            realRigDescription = "Fender Custom Shop 'The Black One' Strat -> Klon Centaur -> Two-Rock Custom Reverb Signature + Dumble Overdrive Special",
            preset = Mk20Preset(
                title = "Gravity",
                style = "Modern Blues / Clean Breakup",
                bpm = 90,
                youtubeLink = "https://www.youtube.com/watch?v=7VBex8zbDRs",
                details = "O timbre de clean/breakup mais desejado dos últimos 20 anos: Klon Centaur fornecendo médios harmônicos em amplificador estilo Two-Rock, compressão e Plate Reverb sedoso.",
                pedals = Mk20Pedals(
                    FX = PedalModule("Pré-Efeito / Dinâmica", true, "Compress", mapOf("Sustain" to 5.0f, "Attack" to 40f, "Level" to 70f, "Blend" to 75f)),
                    NS = PedalModule("Noise Suppressor", true, "soft gate", mapOf("thd" to -62f, "inputDetect" to 0f)),
                    DS = PedalModule("Drive / Boost", true, "MW-GOLD", mapOf("gain" to 25f, "tone" to 52f, "level" to 85f)),
                    AMP = PedalModule("Amplificador", true, "13CL1_FenDi", mapOf("Gain" to 36f, "Level" to 78f, "Bass" to 54f, "Middle" to 62f, "Treble" to 64f, "Reso" to 50f, "Pres" to 54f, "Bright" to 50f)),
                    CAB = PedalModule("Caixa / IR", true, "20FdTwRev212A", mapOf("Level" to 80f, "Low Cut" to 75f, "High Cut" to 7500f)),
                    EQ = PedalModule("Equalizador", true, "Guitar EQ 6", mapOf("100Hz" to 0f, "200Hz" to 1f, "400Hz" to -2f, "800Hz" to 1f, "1.6kHz" to 2f, "3.2kHz" to 2f)),
                    MOD = PedalModule("Modulação", false, "CHORUS", mapOf("speed" to 1.0f, "depth" to 45f, "mix" to 35f, "sync" to 0f, "syncBpm" to 90f)),
                    DELAY = PedalModule("Delay", true, "Analog", mapOf("Time" to 380f, "Fb" to 26f, "Mix" to 20f, "Sync" to 0f, "SyncBPM" to 90f)),
                    REVERB = PedalModule("Reverb", true, "plate", mapOf("decay" to 35f, "mix" to 22f, "highPass" to 80f, "lowPass" to 80f, "modDepth" to 10f))
                )
            )
        ),
        MasterSongItem(
            id = "crazy_train",
            title = "Crazy Train",
            artist = "Ozzy Osbourne (Randy Rhoads)",
            style = "80s Heavy Metal",
            bpm = 138,
            realRigDescription = "Gibson Les Paul Custom 1974 -> MXR Distortion+ -> Marshall 1959 Super Lead White Mod + MXR Stereo Chorus",
            preset = Mk20Preset(
                title = "Crazy Train",
                style = "80s Metal / Lead",
                bpm = 138,
                youtubeLink = "https://www.youtube.com/watch?v=tMDFv5m18Pw",
                details = "O ataque metálico cortante de Randy Rhoads: distorção MXR empurrando Marshall Plexi com agudos abertos, chorus estéreo dando dimensão e reverb plate.",
                pedals = Mk20Pedals(
                    FX = PedalModule("Pré-Efeito / Dinâmica", false, "Compress", mapOf("Sustain" to 3.5f, "Attack" to 50f, "Level" to 70f, "Blend" to 50f)),
                    NS = PedalModule("Noise Suppressor", true, "soft gate", mapOf("thd" to -55f, "inputDetect" to 0f)),
                    DS = PedalModule("Drive / Boost", true, "MW-DS1", mapOf("gain" to 45f, "tone" to 62f, "level" to 90f)),
                    AMP = PedalModule("Amplificador", true, "51OD1_MS2203", mapOf("Gain" to 68f, "Level" to 76f, "Bass" to 50f, "Middle" to 68f, "Treble" to 72f, "Reso" to 50f, "Pres" to 70f, "Bright" to 75f)),
                    CAB = PedalModule("Caixa / IR", true, "33Mar60A412A", mapOf("Level" to 82f, "Low Cut" to 85f, "High Cut" to 6800f)),
                    EQ = PedalModule("Equalizador", true, "Guitar EQ 6", mapOf("100Hz" to -1f, "200Hz" to -2f, "400Hz" to -2f, "800Hz" to 3f, "1.6kHz" to 4f, "3.2kHz" to 3f)),
                    MOD = PedalModule("Modulação", true, "CHORUS STEREO", mapOf("speed" to 0.8f, "depth" to 35f, "mode" to 1f, "mix" to 28f, "sync" to 0f, "syncBpm" to 138f)),
                    DELAY = PedalModule("Delay", true, "Analog", mapOf("Time" to 215f, "Fb" to 20f, "Mix" to 18f, "Sync" to 0f, "SyncBPM" to 138f)),
                    REVERB = PedalModule("Reverb", true, "plate", mapOf("decay" to 25f, "mix" to 18f, "highPass" to 80f, "lowPass" to 80f, "modDepth" to 10f))
                )
            )
        ),
        MasterSongItem(
            id = "hysteria_bass",
            title = "Hysteria (Baixo)",
            artist = "Muse (Chris Wolstenholme)",
            style = "Bass Fuzz",
            bpm = 118,
            realRigDescription = "Status Graphite Bass -> Electro-Harmonix Russian Big Muff + Marshall DBS7400 / Ampeg SVT",
            preset = Mk20Preset(
                title = "Hysteria (Baixo)",
                style = "Bass / Fuzz Distortion",
                bpm = 118,
                youtubeLink = "https://www.youtube.com/watch?v=3dm_5qWWDV8",
                details = "O riff de contrabaixo mais monstruoso do rock: Darkglass B7K saturado misturado com Ampeg SVT 8x10 e equalização de 7 bandas destacando os 800Hz e 4.5kHz.",
                pedals = Mk20Pedals(
                    FX = PedalModule("Pré-Efeito / Dinâmica", true, "Compress Pro", mapOf("Ratio" to 5.0f, "Gain" to 55f, "Knee" to 35f, "Thd" to 50f, "Attack" to 35f, "Level" to 75f, "Blend" to 80f)),
                    NS = PedalModule("Noise Suppressor", true, "soft gate", mapOf("thd" to -50f, "inputDetect" to 0f)),
                    DS = PedalModule("Drive / Boost", true, "MW-B7000", mapOf("gain" to 65f, "level" to 60f, "blend" to 65f, "grunt" to 2f, "attack" to 2f, "master" to 55f, "bass" to 58f, "lomid freq" to 2f, "lomid" to 54f, "himid freq" to 2f, "himid" to 62f, "treble" to 55f)),
                    AMP = PedalModule("Amplificador", true, "102ApSVT_BS", mapOf("Gain" to 55f, "Level" to 80f, "Bass" to 68f, "Middle" to 60f, "Treble" to 55f, "Reso" to 70f, "Pres" to 58f, "Bright" to 0f)),
                    CAB = PedalModule("Caixa / IR", true, "80AmpgSVT810", mapOf("Level" to 85f, "Low Cut" to 35f, "High Cut" to 5200f)),
                    EQ = PedalModule("Equalizador", true, "Bass EQ 7", mapOf("50Hz" to 2f, "120Hz" to 3f, "400Hz" to -2f, "500Hz" to -1f, "800Hz" to 2f, "4.5kHz" to 3f, "10k" to 0f)),
                    MOD = PedalModule("Modulação", false, "CHORUS", mapOf("speed" to 1.0f, "depth" to 50f, "mix" to 50f, "sync" to 0f, "syncBpm" to 118f)),
                    DELAY = PedalModule("Delay", false, "Analog", mapOf("Time" to 350f, "Fb" to 20f, "Mix" to 15f, "Sync" to 0f, "SyncBPM" to 118f)),
                    REVERB = PedalModule("Reverb", true, "room", mapOf("decay" to 15f, "mix" to 14f, "highPass" to 80f, "lowPass" to 80f, "modDepth" to 8f))
                )
            )
        ),
        MasterSongItem(
            id = "little_wing",
            title = "Little Wing",
            artist = "Jimi Hendrix",
            style = "Psychedelic Blues",
            bpm = 72,
            realRigDescription = "Fender Stratocaster 1968 -> Marshall JTM45 100W -> Shin-ei Uni-Vibe + Dallas Arbiter Fuzz Face",
            preset = Mk20Preset(
                title = "Little Wing",
                style = "Psychedelic Blues / Breakup",
                bpm = 72,
                youtubeLink = "https://www.youtube.com/watch?v=DfxvTzG8j_k",
                details = "A doçura orgânica de Jimi: amplificador valvulado clássico no ponto doce de breakup, modulação Uni-Vibe espacial e Spring Reverb de estúdio.",
                pedals = Mk20Pedals(
                    FX = PedalModule("Pré-Efeito / Dinâmica", true, "Compress", mapOf("Sustain" to 4.8f, "Attack" to 45f, "Level" to 70f, "Blend" to 65f)),
                    NS = PedalModule("Noise Suppressor", false, "soft gate", mapOf("thd" to -60f, "inputDetect" to 0f)),
                    DS = PedalModule("Drive / Boost", false, "MW-FACE", mapOf("fuzz" to 45f, "level" to 80f)),
                    AMP = PedalModule("Amplificador", true, "82OD_VOSAC30", mapOf("Gain" to 48f, "Level" to 76f, "Bass" to 56f, "Middle" to 64f, "Treble" to 66f, "Reso" to 50f, "Pres" to 58f, "Bright" to 60f)),
                    CAB = PedalModule("Caixa / IR", true, "64VAC30TBC212", mapOf("Level" to 82f, "Low Cut" to 75f, "High Cut" to 7200f)),
                    EQ = PedalModule("Equalizador", true, "Guitar EQ 6", mapOf("100Hz" to 1f, "200Hz" to 0f, "400Hz" to -2f, "800Hz" to 2f, "1.6kHz" to 3f, "3.2kHz" to 2f)),
                    MOD = PedalModule("Modulação", true, "UNIVIBE", mapOf("speed" to 1.4f, "depth" to 55f, "mix" to 45f, "sync" to 0f, "syncBpm" to 72f)),
                    DELAY = PedalModule("Delay", false, "Echo", mapOf("Time" to 415f, "Fb" to 25f, "Mix" to 20f, "Sync" to 0f, "SyncBPM" to 72f)),
                    REVERB = PedalModule("Reverb", true, "spring", mapOf("decay" to 35f, "mix" to 24f, "highPass" to 80f, "lowPass" to 80f, "combs" to 50f))
                )
            )
        ),
        MasterSongItem(
            id = "way_maker_worship",
            title = "Way Maker / Modern Worship Lead",
            artist = "Worship / Elevation / Bethel",
            style = "Worship Ambient",
            bpm = 72,
            realRigDescription = "Gretsch Duo Jet / Duesenberg -> Klon Centaur + KOT -> Vox AC30 Top Boost + Strymon Big Sky + Timeline",
            preset = Mk20Preset(
                title = "Way Maker (Worship Lead)",
                style = "Worship / Ambient Lead",
                bpm = 72,
                youtubeLink = "https://www.youtube.com/watch?v=iJCV_2H9xD0",
                details = "O timbre definitivo de guitarra de louvor: Vox AC30 Top Boost com Klon suave, delay em colcheia pontuada e Shimmer Reverb etéreo preenchendo todo o ambiente.",
                pedals = Mk20Pedals(
                    FX = PedalModule("Pré-Efeito / Dinâmica", true, "Compress", mapOf("Sustain" to 5.0f, "Attack" to 42f, "Level" to 70f, "Blend" to 70f)),
                    NS = PedalModule("Noise Suppressor", true, "soft gate", mapOf("thd" to -60f, "inputDetect" to 0f)),
                    DS = PedalModule("Drive / Boost", true, "MW-GOLD", mapOf("gain" to 25f, "tone" to 52f, "level" to 85f)),
                    AMP = PedalModule("Amplificador", true, "82OD_VOSAC30", mapOf("Gain" to 42f, "Level" to 76f, "Bass" to 50f, "Middle" to 64f, "Treble" to 66f, "Reso" to 50f, "Pres" to 60f, "Bright" to 75f)),
                    CAB = PedalModule("Caixa / IR", true, "64VAC30TBC212", mapOf("Level" to 82f, "Low Cut" to 80f, "High Cut" to 7200f)),
                    EQ = PedalModule("Equalizador", true, "Guitar EQ 6", mapOf("100Hz" to -1f, "200Hz" to 0f, "400Hz" to -2f, "800Hz" to 1f, "1.6kHz" to 3f, "3.2kHz" to 3f)),
                    MOD = PedalModule("Modulação", false, "CHORUS STEREO", mapOf("speed" to 1.0f, "depth" to 40f, "mode" to 1f, "mix" to 35f, "sync" to 0f, "syncBpm" to 72f)),
                    DELAY = PedalModule("Delay", true, "Dual", mapOf("Time" to 416f, "Fb" to 42f, "Mix" to 30f, "T-Mode" to 2f, "Speed" to 10f, "Depth" to 15f, "Sync" to 0f, "SyncBPM" to 72f)),
                    REVERB = PedalModule("Reverb", true, "shimmer stereo", mapOf("decay" to 65f, "mix" to 30f, "tone" to 70f, "pitch" to 40f, "amount" to 32f))
                )
            )
        ),
        MasterSongItem(
            id = "the_kill",
            title = "The Kill (Bury Me)",
            artist = "30 Seconds to Mars",
            style = "Alternative Rock / Post-Grunge",
            bpm = 183,
            realRigDescription = "Gibson Les Paul Custom -> Mesa Boogie Triple Rectifier + Marshall 1960A Celestion V30 + TS-808 Clean Boost + Plate Reverb",
            preset = Mk20Preset(
                title = "The Kill (Bury Me)",
                style = "Alternative Rock / Post-Grunge",
                bpm = 183,
                youtubeLink = "https://www.youtube.com/watch?v=3c4_jTnsegM",
                details = "Timbre oficial de 'The Kill (Bury Me)' de 30 Seconds to Mars: Mesa Boogie Rectifier com ganho na medida certa, empurrado por TS-808 (level alto, drive em 12) para aperto nos graves e ataque veloz. Reverb de placa estúdio de 17% e delay analógico de 328ms com cauda curta e sem embolamento.",
                pedals = Mk20Pedals(
                    FX = PedalModule("Pré-Efeito / Dinâmica", true, "Compress", mapOf("Sustain" to 3.5f, "Attack" to 50f, "Level" to 74f, "Blend" to 55f)),
                    NS = PedalModule("Noise Suppressor", true, "ai ms gate", mapOf("gate" to 52f, "bias" to 50f, "inputDetect" to 0f)),
                    DS = PedalModule("Drive / Boost", true, "MW-808", mapOf("gain" to 12f, "tone" to 58f, "level" to 90f)),
                    AMP = PedalModule("Amplificador", true, "70OD2_meas", mapOf("Gain" to 66f, "Level" to 74f, "Bass" to 56f, "Middle" to 62f, "Treble" to 58f, "Reso" to 54f, "Pres" to 55f, "Bright" to 50f)),
                    CAB = PedalModule("Caixa / IR", true, "46MbOS412A", mapOf("Level" to 82f, "Low Cut" to 85f, "High Cut" to 6800f)),
                    EQ = PedalModule("Equalizador", true, "Guitar EQ 6", mapOf("100Hz" to 0f, "200Hz" to -1f, "400Hz" to -2f, "800Hz" to 2f, "1.6kHz" to 3f, "3.2kHz" to 1f)),
                    MOD = PedalModule("Modulação", false, "CHORUS", mapOf("speed" to 1.2f, "depth" to 35f, "mix" to 25f, "sync" to 0f, "syncBpm" to 183f)),
                    DELAY = PedalModule("Delay", true, "Analog", mapOf("Time" to 328f, "Fb" to 22f, "Mix" to 18f, "Sync" to 0f, "SyncBPM" to 183f)),
                    REVERB = PedalModule("Reverb", true, "plate", mapOf("decay" to 22f, "mix" to 17f, "highPass" to 80f, "lowPass" to 80f, "modDepth" to 8f))
                )
            )
        ),
        MasterSongItem(
            id = "o_escudo",
            title = "O Escudo",
            artist = "Voz da Verdade",
            style = "Gospel Rock / Melodic Solo",
            bpm = 132,
            realRigDescription = "Fender Stratocaster / Ibanez (Humbucker Ponte) -> Tube Screamer TS-808 -> Marshall JCM800 + Caixa 1960A V30 + Analog Delay",
            preset = Mk20Preset(
                title = "O Escudo - Voz da Verdade",
                style = "Gospel Rock / Melodic Solo",
                bpm = 132,
                masterVolume = 65,
                youtubeLink = "https://www.youtube.com/watch?v=o_escudo",
                details = "[Matriz Oficial: Solo Melódico & Gospel Lead] Timbre oficial de 'O Escudo' (Voz da Verdade): Marshall JCM800 com Bright ativo (68), empurrado por Tube Screamer MW-808, compressor de estúdio para sustain cantante sem microfonia e High Cut aberto em 7.8kHz com todo o brilho original.",
                pedals = Mk20Pedals(
                    FX = PedalModule("Pré-Efeito / Dinâmica", true, "Compress", mapOf("Sustain" to 4.5f, "Attack" to 48f, "Level" to 68f, "Blend" to 55f)),
                    NS = PedalModule("Noise Suppressor", true, "soft gate", mapOf("thd" to -52f, "inputDetect" to 0f)),
                    DS = PedalModule("Drive / Boost", true, "MW-808", mapOf("gain" to 20f, "tone" to 56f, "level" to 90f)),
                    AMP = PedalModule("Amplificador", true, "86OD_JCM800", mapOf("Gain" to 58f, "Level" to 78f, "Bass" to 54f, "Middle" to 66f, "Treble" to 66f, "Reso" to 56f, "Pres" to 62f, "Bright" to 68f)),
                    CAB = PedalModule("Caixa / IR", true, "33Mar60A412A", mapOf("Level" to 85f, "Low Cut" to 78f, "High Cut" to 7800f)),
                    EQ = PedalModule("Equalizador", true, "Guitar EQ 6", mapOf("100Hz" to 1f, "200Hz" to 0f, "400Hz" to -1f, "800Hz" to 2f, "1.6kHz" to 2f, "3.2kHz" to 2f)),
                    MOD = PedalModule("Modulação", false, "CHORUS", mapOf("speed" to 1.0f, "depth" to 38f, "mix" to 28f, "sync" to 0f, "syncBpm" to 132f)),
                    DELAY = PedalModule("Delay", true, "Analog", mapOf("Time" to 360f, "Fb" to 28f, "Mix" to 22f, "Sync" to 0f, "SyncBPM" to 132f)),
                    REVERB = PedalModule("Reverb", true, "plate", mapOf("decay" to 24f, "mix" to 18f, "highPass" to 80f, "lowPass" to 80f, "modDepth" to 10f))
                )
            )
        ),
        MasterSongItem(
            id = "deus_tremendo",
            title = "Deus Tremendo",
            artist = "Voz da Verdade",
            style = "Gospel Rock / Brass & Lead",
            bpm = 128,
            realRigDescription = "Fender Stratocaster HSS -> Tube Screamer TS-808 -> Marshall JCM800 2203 + Caixa 1960A 4x12 + Plate Reverb",
            preset = Mk20Preset(
                title = "Deus Tremendo - Voz da Verdade",
                style = "Gospel Rock / Melodic Solo",
                bpm = 128,
                masterVolume = 65,
                youtubeLink = "https://www.youtube.com/watch?v=deus_tremendo",
                details = "[Matriz Oficial: Solo Melódico & Gospel Lead] Timbre oficial de 'Deus Tremendo' (Voz da Verdade): Ataque definido nos metais e solo encorpado com Marshall JCM800, brilho aberto em 7.8kHz, Noise Gate transparente (-52dB) e zero microfonia.",
                pedals = Mk20Pedals(
                    FX = PedalModule("Pré-Efeito / Dinâmica", true, "Compress", mapOf("Sustain" to 4.4f, "Attack" to 48f, "Level" to 68f, "Blend" to 55f)),
                    NS = PedalModule("Noise Suppressor", true, "soft gate", mapOf("thd" to -52f, "inputDetect" to 0f)),
                    DS = PedalModule("Drive / Boost", true, "MW-808", mapOf("gain" to 18f, "tone" to 56f, "level" to 90f)),
                    AMP = PedalModule("Amplificador", true, "86OD_JCM800", mapOf("Gain" to 56f, "Level" to 78f, "Bass" to 54f, "Middle" to 66f, "Treble" to 65f, "Reso" to 55f, "Pres" to 60f, "Bright" to 68f)),
                    CAB = PedalModule("Caixa / IR", true, "33Mar60A412A", mapOf("Level" to 85f, "Low Cut" to 78f, "High Cut" to 7800f)),
                    EQ = PedalModule("Equalizador", true, "Guitar EQ 6", mapOf("100Hz" to 1f, "200Hz" to 0f, "400Hz" to -1f, "800Hz" to 2f, "1.6kHz" to 2f, "3.2kHz" to 2f)),
                    MOD = PedalModule("Modulação", false, "CHORUS", mapOf("speed" to 1.0f, "depth" to 38f, "mix" to 28f, "sync" to 0f, "syncBpm" to 128f)),
                    DELAY = PedalModule("Delay", true, "Analog", mapOf("Time" to 350f, "Fb" to 26f, "Mix" to 20f, "Sync" to 0f, "SyncBPM" to 128f)),
                    REVERB = PedalModule("Reverb", true, "plate", mapOf("decay" to 22f, "mix" to 18f, "highPass" to 80f, "lowPass" to 80f, "modDepth" to 10f))
                )
            )
        )
    )

    fun findSong(query: String): MasterSongItem? {
        val q = query.lowercase().trim()
        if (q.isBlank()) return null
        return SONGS.firstOrNull { item ->
            item.title.lowercase().contains(q) ||
            q.contains(item.title.lowercase()) ||
            item.artist.lowercase().contains(q) && q.length > 10 ||
            item.id.lowercase() == q ||
            (q.contains("o escudo") && item.id == "o_escudo") ||
            (q.contains("deus tremendo") && item.id == "deus_tremendo") ||
            (q.contains("3c4_jtnsegm") && item.id == "the_kill") ||
            (q.contains("the kill") && item.id == "the_kill") ||
            (q.contains("30 seconds to mars") && item.id == "the_kill") ||
            (item.preset.youtubeLink.isNotBlank() && item.preset.youtubeLink.lowercase().contains(q))
        }
    }

    fun findBestMatch(query: String): Mk20Preset? {
        val match = findSong(query)
        return match?.preset
    }
}
