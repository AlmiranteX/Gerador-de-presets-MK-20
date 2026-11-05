package com.example.data.model

/**
 * Catálogo Oficial da pedaleira M-VAVE MK-20 (Firmware v85) extraído dos manuais técnicos oficiais:
 * - FX: 20 modelos
 * - NS: 8 modelos
 * - DS / MW: 52 modelos
 * - AMP: 120 modelos (8 parâmetros globais)
 * - CAB: 100 modelos (3 parâmetros: Level, Low Cut, High Cut)
 * - EQ: 3 modelos (Guitar EQ 6, Bass EQ 7, Normal EQ 10)
 * - MOD: 20 modelos
 * - DLY: 29 modelos
 * - REV: 19 modelos
 */
data class CatalogItem(
    val id: Int,
    val name: String,
    val type: String,
    val description: String,
    val defaultParams: Map<String, Float> = emptyMap()
)

object Mk20Catalog {

    private fun ampDefaults(
        type: String,
        gain: Float = when (type) {
            "Clean" -> 28f
            "Overdrive" -> 55f
            "Distortion" -> 72f
            "Bass" -> 35f
            else -> 50f
        },
        level: Float = when (type) {
            "Clean" -> 75f
            "Bass" -> 78f
            else -> 68f
        },
        bass: Float = when (type) {
            "Clean" -> 48f
            "Bass" -> 65f
            else -> 55f
        },
        middle: Float = when (type) {
            "Distortion" -> 48f
            "Overdrive" -> 60f
            else -> 52f
        },
        treble: Float = when (type) {
            "Clean" -> 58f
            "Bass" -> 52f
            else -> 60f
        },
        reso: Float = when (type) {
            "Clean" -> 45f
            "Bass" -> 70f
            "Distortion" -> 65f
            else -> 55f
        },
        pres: Float = when (type) {
            "Distortion" -> 65f
            "Bass" -> 55f
            else -> 52f
        },
        bright: Float = when (type) {
            "Clean" -> 65f
            "Overdrive" -> 45f
            else -> 0f
        }
    ): Map<String, Float> = mapOf(
        "Gain" to gain,
        "Level" to level,
        "Bass" to bass,
        "Middle" to middle,
        "Treble" to treble,
        "Reso" to reso,
        "Pres" to pres,
        "Bright" to bright
    )

    private fun cabDefaults(config: String): Map<String, Float> = when {
        config.startsWith("Bass", ignoreCase = true) -> mapOf("Level" to 85f, "Low Cut" to 35f, "High Cut" to 5200f)
        config.equals("Acoustic", ignoreCase = true) -> mapOf("Level" to 82f, "Low Cut" to 60f, "High Cut" to 12000f)
        else -> mapOf("Level" to 82f, "Low Cut" to 80f, "High Cut" to 7000f)
    }

    val AMP_MODELS = listOf(
        CatalogItem(1, "1CL1_410H", "Clean", "Marshall JVM410H • Clean britânico clássico com dinâmica natural e médios encorpados", ampDefaults("Clean")),
        CatalogItem(2, "2CL2_410H", "Clean", "Marshall JVM410H • Clean brilhante e transparente com riqueza harmônica para dedilhados", ampDefaults("Clean", bright = 75f)),
        CatalogItem(3, "3CR1_410H", "Overdrive", "Marshall JVM410H • Overdrive leve, quente e macio com dinâmica responsiva ao toque", ampDefaults("Overdrive", gain = 42f)),
        CatalogItem(4, "4CR2_410H", "Overdrive", "Marshall JVM410H • Crunch de médios cheios, firme e potente para rock clássico", ampDefaults("Overdrive", gain = 52f, middle = 64f)),
        CatalogItem(5, "5CR3_410H", "Overdrive", "Marshall JVM410H • Crunch de alta saturação com forte impacto para power chords e hard rock", ampDefaults("Overdrive", gain = 62f)),
        CatalogItem(6, "6OD1_410H", "Overdrive", "Marshall JVM410H • Overdrive britânico tradicional com sustain longo para solos expressivos", ampDefaults("Overdrive", gain = 65f)),
        CatalogItem(7, "7OD2_410H", "Overdrive", "Marshall JVM410H • Overdrive moderno de alto ganho com agudos cortantes para rock e metal", ampDefaults("Overdrive", gain = 70f, treble = 65f)),
        CatalogItem(8, "8OD3_410H", "Overdrive", "Marshall JVM410H • High-gain com médios densos e som encorpado para banda ao vivo", ampDefaults("Overdrive", gain = 74f, middle = 65f)),
        CatalogItem(9, "9OD_AXE1", "Overdrive", "Danelectro Wasabi AX-1 • Overdrive estilo vintage fuzz com textura arenosa de garage rock", ampDefaults("Overdrive", gain = 58f)),
        CatalogItem(10, "10OD1_BLUE", "Overdrive", "BOSS BD-2 Blues Driver • Overdrive clássico de blues com graves quentes e médios redondos", ampDefaults("Overdrive", gain = 45f, bass = 58f)),
        CatalogItem(11, "11OD2_BLUE", "Overdrive", "BOSS BD-2 Blues Driver • Variação com médios destacados e corte afiado na mix", ampDefaults("Overdrive", gain = 50f, middle = 66f)),
        CatalogItem(12, "12DARK_MATTER1", "Overdrive", "TC Electronic Dark Matter • Overdrive de alto ganho com timbre escuro para doom e stoner", ampDefaults("Overdrive", gain = 68f, bass = 64f, treble = 48f)),
        CatalogItem(13, "13CL1_FenDi", "Clean", "Fender 65 Deluxe Reverb • Clean Blackface clássico, doce e aveludado para surf e country", ampDefaults("Clean", gain = 30f, bass = 52f, treble = 64f)),
        CatalogItem(14, "14CL2_FenDi", "Clean", "Fender 65 Deluxe Reverb • Clean brilhante e aberto com separação nítida para fingerstyle", ampDefaults("Clean", gain = 26f, treble = 68f, bright = 80f)),
        CatalogItem(15, "15CL3_FenDi", "Clean", "Fender 65 Deluxe Reverb • Clean vintage macio com palco sonoro relaxado para folk e indie", ampDefaults("Clean", gain = 32f, bright = 55f)),
        CatalogItem(16, "16HORIZON_1", "Overdrive", "Horizon Devices • Overdrive de ganho moderno compacto com graves contidos para djent", ampDefaults("Overdrive", gain = 68f, bass = 45f, pres = 65f)),
        CatalogItem(17, "17HORIZON_2", "Overdrive", "Horizon Devices • Overdrive de ritmo pesado com médios agressivos para metalcore", ampDefaults("Overdrive", gain = 72f, middle = 64f, pres = 65f)),
        CatalogItem(18, "18HVMetal_1", "Distortion", "BOSS Metal Zone • Distorção heavy metal com graves profundos e agudos cortantes", ampDefaults("Distortion", gain = 78f)),
        CatalogItem(19, "19CL1_J900", "Clean", "Marshall JCM900 • Clean britânico firme com compressão natural e ataque definido", ampDefaults("Clean", gain = 30f)),
        CatalogItem(20, "20CL2_J900", "Clean", "Marshall JCM900 • Clean quente e redondo para bases melódicas e arpejos", ampDefaults("Clean", gain = 32f, bass = 54f)),
        CatalogItem(21, "21CL3_J900", "Clean", "Marshall JCM900 • Clean aberto e dinâmico com excelente resposta a pedais", ampDefaults("Clean", gain = 28f)),
        CatalogItem(22, "22OD1_J900", "Overdrive", "Marshall JCM900 • Overdrive hard rock anos 80/90 com médios rasgados e presença", ampDefaults("Overdrive", gain = 62f)),
        CatalogItem(23, "23DS1_J900", "Distortion", "Marshall JCM900 • Distorção de alto ganho para glam metal e solos virtuosos", ampDefaults("Distortion", gain = 72f)),
        CatalogItem(24, "24DS2_J900", "Distortion", "Marshall JCM900 • Distorção pesada moderna com sustentação prolongada", ampDefaults("Distortion", gain = 76f)),
        CatalogItem(25, "25CL1_JC120", "Clean", "Roland JC-120 Jazz Chorus • Clean transistorizado estéreo ultralímpido para jazz e fusion", ampDefaults("Clean", gain = 22f, level = 78f, bright = 75f)),
        CatalogItem(26, "26CL2_JC120", "Clean", "Roland JC-120 Jazz Chorus • Clean clássico com modulação espacial brilhante para pop e worship", ampDefaults("Clean", gain = 25f, level = 78f, bright = 80f)),
        CatalogItem(27, "27OD1_JHS", "Overdrive", "JHS Loud Is More Good • Overdrive transparente boutique que preserva a dinâmica original", ampDefaults("Overdrive", gain = 45f)),
        CatalogItem(28, "28OD2_JHS", "Overdrive", "JHS Loud Is More Good • Overdrive com reforço de médios que destaca a guitarra rítmica", ampDefaults("Overdrive", gain = 50f, middle = 65f)),
        CatalogItem(29, "29OD3_JHS", "Overdrive", "JHS Loud Is More Good • Overdrive quente e suave de alta resposta para solos de blues e fusion", ampDefaults("Overdrive", gain = 55f)),
        CatalogItem(30, "30DS_JHS", "Distortion", "JHS Loud Is More Good • Distorção de alto ganho com ataque nítido para rock contemporâneo", ampDefaults("Distortion", gain = 68f)),
        CatalogItem(31, "31OD1_JOYO", "Overdrive", "JOYO JF-01 Vintage OD • Circuito clássico estilo TS com médios doces e transição suave", ampDefaults("Overdrive", gain = 48f, middle = 62f)),
        CatalogItem(32, "32DS1_JR7W", "Distortion", "Boss KTN-MINI-W • Distorção compacta simulando mini-amp para estudos e gravação", ampDefaults("Distortion", gain = 65f)),
        CatalogItem(33, "33OD1_JVMcrunch", "Overdrive", "Marshall JVM410H Crunch • Crunch comprimido para palhetadas rápidas no thrash metal", ampDefaults("Overdrive", gain = 64f, reso = 62f)),
        CatalogItem(34, "34OD2_JVMcrunch", "Overdrive", "Marshall JVM410H Crunch • Crunch de alto impacto com graves vigorosos para bases pesadas", ampDefaults("Overdrive", gain = 68f, bass = 60f, reso = 65f)),
        CatalogItem(35, "35DS1_Kohle", "Distortion", "Kohle HM2 Singing Mids • Timbre clássico sueco de death metal estilo serra-elétrica", ampDefaults("Distortion", gain = 80f, middle = 70f, treble = 68f)),
        CatalogItem(36, "36DS2_Kohle", "Distortion", "Kohle HM2 Singing Mids • Variação com médios controlados e graves firmes para death metal", ampDefaults("Distortion", gain = 78f, bass = 62f, middle = 55f)),
        CatalogItem(37, "37KohleHM", "Distortion", "Kohle HM2 Singing Mids • Ganho extremo e agressivo para grindcore e black metal", ampDefaults("Distortion", gain = 85f, treble = 70f)),
        CatalogItem(38, "38DS1_M-VAVE", "Distortion", "M-VAVE Custom DS • Distorção moderna neutra e equilibrada versátil para metal", ampDefaults("Distortion", gain = 70f)),
        CatalogItem(39, "39OD1_M-VAVE", "Overdrive", "M-VAVE Custom DS • Overdrive digital transparente de resposta rápida para pop e rock", ampDefaults("Overdrive", gain = 46f)),
        CatalogItem(40, "40DS2_M-VAVE", "Distortion", "M-VAVE Custom DS • Distorção pesada com subgraves profundos para breakdowns de metalcore", ampDefaults("Distortion", gain = 76f, bass = 64f, reso = 70f)),
        CatalogItem(41, "41CL1_M-VAVE", "Clean", "M-VAVE Custom OD • Sinal limpo bufferizado de alta fidelidade para cadeia de efeitos", ampDefaults("Clean", gain = 20f)),
        CatalogItem(42, "42OD2_M-VAVE", "Overdrive", "M-VAVE Custom OD • Overdrive vintage macio e aveludado para baladas e solos retrô", ampDefaults("Overdrive", gain = 50f)),
        CatalogItem(43, "43CL2_M-VAVE", "Clean", "M-VAVE Custom OD • Clean ligeiramente quebrado (edge-of-breakup) para country e pop", ampDefaults("Clean", gain = 36f)),
        CatalogItem(44, "44OD3_M-VAVE", "Overdrive", "M-VAVE Custom OD • Overdrive de alto ganho com médios cheios próximo de distorção", ampDefaults("Overdrive", gain = 62f, middle = 62f)),
        CatalogItem(45, "45CL3_M-VAVE", "Clean", "M-VAVE Custom OD • Clean brilhante de agudos proeminentes para captadores single-coil", ampDefaults("Clean", gain = 25f, treble = 66f, bright = 75f)),
        CatalogItem(46, "46OD3_M-VAVE", "Overdrive", "M-VAVE Custom OD • Overdrive de alta definição com graves contidos e separação harmônica", ampDefaults("Overdrive", gain = 58f, bass = 45f)),
        CatalogItem(47, "47CL4_M-VAVE", "Clean", "M-VAVE Custom OD • Clean encorpado com ênfase no grave para passagens contrastantes", ampDefaults("Clean", gain = 28f, bass = 62f)),
        CatalogItem(48, "48CH1_ME", "Overdrive", "Mesa Boogie JP-2C • Timbre clássico Mark com ataque imediato e médios refinados", ampDefaults("Overdrive", gain = 60f, middle = 58f)),
        CatalogItem(49, "49CH2_ME", "Overdrive", "Mesa Boogie JP-2C • Canal de alto ganho com ultra-clareza para solos de prog metal", ampDefaults("Overdrive", gain = 72f, middle = 62f, treble = 64f)),
        CatalogItem(50, "50DS1_metal", "Distortion", "Blackstar HT Metal 60 • Distorção valvulada moderna de alto ganho e compressão firme", ampDefaults("Distortion", gain = 75f)),
        CatalogItem(51, "51OD1_MS2203", "Overdrive", "Marshall JCM800 2203 • O lendário Brown Sound explosivo do hard rock e thrash clássico", ampDefaults("Overdrive", gain = 65f, middle = 64f, treble = 62f)),
        CatalogItem(52, "52OD1_NUX", "Overdrive", "NUX Overdrive • Overdrive macio e perdoador estilo TS para blues e rock", ampDefaults("Overdrive", gain = 48f)),
        CatalogItem(53, "53OD2_NUX", "Overdrive", "NUX Overdrive • Overdrive de saída elevada e agudos abertos para presença ao vivo", ampDefaults("Overdrive", gain = 56f, treble = 64f)),
        CatalogItem(54, "54CL1_SUPA", "Clean", "Supro 1696RT Black Magick • Clean vintage norte-americano escuro e quente para blues", ampDefaults("Clean", gain = 34f, bass = 56f, treble = 50f)),
        CatalogItem(55, "55CL1_orange", "Clean", "Orange Rockerverb 100 • Clean britânico brilhante e estalado cheio de chime", ampDefaults("Clean", gain = 30f, middle = 58f, treble = 62f)),
        CatalogItem(56, "56OD1_Piranha", "Overdrive", "Pro Co RAT Custom • Overdrive agressivo de alto ganho com corte cru para grunge e punk", ampDefaults("Overdrive", gain = 64f)),
        CatalogItem(57, "57OD1_RAT", "Overdrive", "Pro Co RAT Clássico • Ceifamento característico do RAT com médios destacados para garage", ampDefaults("Overdrive", gain = 58f, middle = 64f)),
        CatalogItem(58, "58OD2_RAT", "Overdrive", "Pro Co RAT Clássico • Variação com saturação expressiva e dinâmica controlável", ampDefaults("Overdrive", gain = 60f)),
        CatalogItem(59, "59DS1_RAT", "Distortion", "Pro Co RAT Distortion • Distorção extrema pesada com alta compressão para hardcore punk", ampDefaults("Distortion", gain = 76f)),
        CatalogItem(60, "60DS1_ROOM", "Distortion", "MPF Sounds Room • Distorção com reverberação espacial natural para pós-rock", ampDefaults("Distortion", gain = 66f)),
        CatalogItem(61, "61DS2_ROOM", "Distortion", "MPF Sounds Room • Médios densos que mantêm peso uniforme em baixos volumes", ampDefaults("Distortion", gain = 68f, middle = 62f)),
        CatalogItem(62, "62DS3_ROOM", "Distortion", "MPF Sounds Room • Timbre encorpado de ambiente com sensação de amp no quarto", ampDefaults("Distortion", gain = 70f, bass = 60f)),
        CatalogItem(63, "63CL_WANG", "Clean", "Wangs VT-18 • Clean valvulado de baixa potência britânico, dinâmico e quente", ampDefaults("Clean", gain = 32f)),
        CatalogItem(64, "64DS1_XC", "Distortion", "Bogner XTC Ecstasy • Distorção high-gain tridimensional para metal progressivo e melódico", ampDefaults("Distortion", gain = 74f, middle = 58f, pres = 65f)),
        CatalogItem(65, "65DS2_XC", "Distortion", "Bogner XTC Ecstasy • Distorção ultra-pesada com graves contidos e foco no ritmo", ampDefaults("Distortion", gain = 78f, bass = 48f, pres = 68f)),
        CatalogItem(66, "66CL1_yuimer", "Clean", "YUIMER OD Clean • Sinal limpo transparente sem perdas para base de modulações", ampDefaults("Clean", gain = 24f)),
        CatalogItem(67, "67CL2_yuimer", "Clean", "YUIMER OD Clean • Clean quente e macio para melodias de pop e rock acústico/elétrico", ampDefaults("Clean", gain = 28f, bass = 54f)),
        CatalogItem(68, "68OD1_yuimer", "Overdrive", "YUIMER OD Overdrive • Overdrive aveludado e musical com médios redondos para solos", ampDefaults("Overdrive", gain = 50f, middle = 60f)),
        CatalogItem(69, "69OD1_meas", "Overdrive", "Mesa Boogie Triple Rectifier • High-gain americano com ataque percussivo e graves poderosos", ampDefaults("Overdrive", gain = 70f, bass = 62f, reso = 68f)),
        CatalogItem(70, "70OD2_meas", "Overdrive", "Mesa Boogie Triple Rectifier • Overdrive de alta compressão projetado para palm mutes", ampDefaults("Overdrive", gain = 74f, bass = 60f, reso = 70f)),
        CatalogItem(71, "71CL1_meas", "Clean", "Mesa Boogie Triple Rectifier • Clean sólido americano de alta dinâmica com breakup natural", ampDefaults("Clean", gain = 30f)),
        CatalogItem(72, "72CL2_meas", "Clean", "Mesa Boogie Triple Rectifier • Clean brilhante de agudos estendidos para melodias nítidas", ampDefaults("Clean", gain = 26f, treble = 65f, bright = 75f)),
        CatalogItem(73, "73CR1_meas", "Overdrive", "Mesa Boogie Triple Rectifier • Crunch clássico americano na transição entre clean e high-gain", ampDefaults("Overdrive", gain = 54f)),
        CatalogItem(74, "74OD_ENGL_120", "Overdrive", "ENGL Savage 120 • Overdrive alemão de corte cirúrgico e rico em harmônicos para speed metal", ampDefaults("Overdrive", gain = 68f, middle = 62f, pres = 66f)),
        CatalogItem(75, "75DS_ENGL_120", "Distortion", "ENGL Savage 120 • Distorção alemã extrema de alto ganho para metal moderno e death metal", ampDefaults("Distortion", gain = 78f, bass = 55f, middle = 52f, treble = 65f, reso = 68f, pres = 68f)),
        CatalogItem(76, "76CL_SoldSolo100", "Clean", "Soldano SLO-100 • Clean boutique americano cristalino e dinâmico para pedais", ampDefaults("Clean", gain = 28f, bright = 70f)),
        CatalogItem(77, "77OD_SoldSolo100", "Overdrive", "Soldano SLO-100 • O famoso crunch dourado do SLO, aveludado e rico para AOR e hard rock", ampDefaults("Overdrive", gain = 58f, middle = 62f)),
        CatalogItem(78, "78DS_SoldSolo100", "Distortion", "Soldano SLO-100 • A referência mundial de alto ganho com definição impecável corda a corda", ampDefaults("Distortion", gain = 74f, middle = 58f, treble = 64f, pres = 65f)),
        CatalogItem(79, "79OD_5150III", "Overdrive", "EVH 5150 III • Assinatura explosiva de Eddie Van Halen com resposta rápida para tapping", ampDefaults("Overdrive", gain = 66f, treble = 64f, pres = 65f)),
        CatalogItem(80, "80DS_5150III", "Distortion", "EVH 5150 III • Padrão do metalcore moderno com graves secos e agudos articulados", ampDefaults("Distortion", gain = 76f, bass = 56f, treble = 65f, reso = 68f, pres = 68f)),
        CatalogItem(81, "81CL_VOSAC30", "Clean", "VOX AC30 Top Boost • Circuito britânico Classe A com harmônicos cintilantes (chime)", ampDefaults("Clean", gain = 32f, treble = 66f, bright = 80f)),
        CatalogItem(82, "82OD_VOSAC30", "Overdrive", "VOX AC30 Top Boost • Overdrive orgânico doce com médios cortantes do britpop e indie", ampDefaults("Overdrive", gain = 54f, middle = 62f, treble = 64f, bright = 60f)),
        CatalogItem(83, "83CL_DiezzIVH4", "Clean", "Diezel VH4 • Clean alemão de precisão, tridimensional e hi-fi para gravações técnicas", ampDefaults("Clean", gain = 26f)),
        CatalogItem(84, "84OD_DiezzIVH4", "Overdrive", "Diezel VH4 • Overdrive firme com médios consistentes e agudos polidos para rock progressivo", ampDefaults("Overdrive", gain = 62f, middle = 60f)),
        CatalogItem(85, "85DS_DiezzIVH4", "Distortion", "Diezel VH4 (Canal 3/4) • O suprassumo do metal pesado alemão com foco em afinações graves e djent", ampDefaults("Distortion", gain = 78f, bass = 58f, reso = 72f, pres = 68f)),
        CatalogItem(86, "86OD_JCM800", "Overdrive", "Marshall JCM800 • Crunch britânico direto e focado, padrão de ouro do rock clássico e heavy metal", ampDefaults("Overdrive", gain = 64f, middle = 66f, treble = 62f, reso = 58f, pres = 60f)),
        CatalogItem(87, "87DS_JCM800", "Distortion", "Marshall JCM800 Modded • Alto ganho britânico modificado com médios agressivos para thrash", ampDefaults("Distortion", gain = 75f, middle = 64f, treble = 64f)),
        CatalogItem(88, "88OD_Peavey6505", "Overdrive", "Peavey 6505 Rhythm • Overdrive com graves musculosos e ataque percussivo para metalcore", ampDefaults("Overdrive", gain = 66f, bass = 60f, reso = 65f)),
        CatalogItem(89, "89DS_Peavey6505", "Distortion", "Peavey 6505 Lead • Distorção padrão da cena de metal moderno com altíssima sustentação", ampDefaults("Distortion", gain = 78f, bass = 58f, reso = 68f, pres = 68f)),
        CatalogItem(90, "90DS1_TONE", "Distortion", "IK Multimedia TONEX • Modelagem de alta fidelidade neutra cobrindo timbres vintage a modernos", ampDefaults("Distortion", gain = 68f)),
        CatalogItem(91, "91AgB659_BS", "Bass", "Aguilar DB659 • Pré-amp clássico de contrabaixo americano valvulado, quente e sedoso", ampDefaults("Bass", gain = 32f, bass = 64f)),
        CatalogItem(92, "92Ag500_BS", "Bass", "Aguilar AG500 • Contrabaixo moderno e veloz com graves limpos e médios destacados para slap", ampDefaults("Bass", gain = 30f, bass = 66f, middle = 58f, treble = 60f)),
        CatalogItem(93, "93Alm1X_BS", "Bass", "Alembic F-1X • Pré-amplificador hi-end cristalino e ultra-detalhado para estúdio e jazz", ampDefaults("Bass", gain = 26f, bright = 60f)),
        CatalogItem(94, "94Ap50_BS", "Bass", "Darkglass Alpha Omega 500 • Distorção moderna pesada de baixo com subgraves viscerais para djent", ampDefaults("Bass", gain = 58f, bass = 68f, treble = 62f, reso = 72f)),
        CatalogItem(95, "95Mar74_BS", "Bass", "Marshall DBS7400 • Baixo de rock britânico com médios projetados e pegada agressiva", ampDefaults("Bass", gain = 42f, middle = 62f)),
        CatalogItem(96, "96PeBC1_Bs", "Bass", "Pearce BC1 • Timbre vintage americano de contrabaixo, elástico e quente para soul e R&B", ampDefaults("Bass", gain = 34f, bass = 64f)),
        CatalogItem(97, "97Th21B_BS", "Bass", "Tech21 Bass Driver DI • Clássica caixa de injeção direta de baixo com overdrive pronto para palco", ampDefaults("Bass", gain = 45f, bass = 65f, treble = 58f)),
        CatalogItem(98, "98Th21VT_BS", "Bass", "Tech21 VT Bass Deluxe • Simulação de amp valvulado de baixo estilo vintage americano", ampDefaults("Bass", gain = 44f, middle = 58f)),
        CatalogItem(99, "99Wal12_BS", "Bass", "Walter Woods Ultra 1200 • Amplificador hi-fi ultralímpido para baixos acústicos e fretless", ampDefaults("Bass", gain = 25f, bright = 55f)),
        CatalogItem(100, "100WwL10_BS", "Bass", "Warwick LWA 1000 • Contrabaixo moderno alemão com graves sólidos e articulados", ampDefaults("Bass", gain = 32f, bass = 66f)),
        CatalogItem(101, "101AgDb750_BS", "Bass", "Aguilar DB750 • Amplificador híbrido potente com graves profundos e médios aveludados", ampDefaults("Bass", gain = 35f, bass = 68f)),
        CatalogItem(102, "102ApSVT_BS", "Bass", "AMPEG SVT-400T • O som clássico valvulado de baixo do Ampeg, gordo e com pressão", ampDefaults("Bass", gain = 40f, bass = 68f, middle = 58f, reso = 72f)),
        CatalogItem(103, "103DgM900_BS", "Bass", "Darkglass Microtubes 900 • Distorção pesada de contrabaixo com definição cirúrgica para metal", ampDefaults("Bass", gain = 56f, bass = 66f, treble = 62f)),
        CatalogItem(104, "104FenRum_BS", "Bass", "Fender Rumble 100 • Clean tradicional americano para contrabaixo, aberto e equilibrado", ampDefaults("Bass", gain = 30f, bass = 62f)),
        CatalogItem(105, "105GkF550_BS", "Bass", "Gallien-Krueger Fusion 550 • Timbre versátil moderno com agudos pontiagudos para slap", ampDefaults("Bass", gain = 34f, bass = 65f, treble = 64f, pres = 62f)),
        CatalogItem(106, "106HkeHd50_BS", "Bass", "Hartke HD50 • Timbre brilhante e focado com cone de alumínio para pop, slap e estúdio", ampDefaults("Bass", gain = 32f, treble = 62f, bright = 60f)),
        CatalogItem(107, "107MarkLm_BS", "Bass", "Markbass Little Mark 250 • Timbre italiano leve e articulado com graves secos e médios presentes", ampDefaults("Bass", gain = 32f, middle = 60f)),
        CatalogItem(108, "108OrgAd_BS", "Bass", "Orange AD200B • Baixo valvulado britânico vintage, rugoso e encorpado para stoner e grunge", ampDefaults("Bass", gain = 48f, bass = 66f, middle = 62f)),
        CatalogItem(109, "109PjBuddy_BS", "Bass", "Phil Jones Bass Buddy • Hi-Fi puro e delicado para gravações acústicas e baixos de 6 cordas", ampDefaults("Bass", gain = 26f)),
        CatalogItem(110, "110RolDb_BS", "Bass", "Roland D-Bass 210 • Tecnologia digital neutra e equilibrada com alta margem dinâmica", ampDefaults("Bass", gain = 30f)),
        CatalogItem(111, "111Mb400C1_BS", "Bass", "Mesa Boogie Bass 400+ • Timbre valvulado americano de alta fidelidade, caloroso e espesso", ampDefaults("Bass", gain = 36f, bass = 66f)),
        CatalogItem(112, "112Mb400C2_BS", "Bass", "Mesa Boogie Bass 400+ • Canal com agudos afiados e médios definidos para metal progressivo", ampDefaults("Bass", gain = 46f, middle = 62f, treble = 62f)),
        CatalogItem(113, "113DgXu_BS", "Bass", "Darkglass X Ultra • Distorção multibanda para baixo separando graves limpos de médios distorcidos", ampDefaults("Bass", gain = 58f, bass = 68f, treble = 64f)),
        CatalogItem(114, "114ApSp_BS", "Bass", "Ampeg BA-210SP • Clássico timbre Ampeg de ensaio, encorpado e musical para pop e funk", ampDefaults("Bass", gain = 34f, bass = 64f)),
        CatalogItem(115, "115Mar50_BS", "Bass", "Marshall SuperBass • Baixo de rock clássico britânico direto e agressivo dos anos 70", ampDefaults("Bass", gain = 45f, middle = 62f)),
        CatalogItem(116, "116Mark500_BS", "Bass", "Markbass TTE500 • Válvula clássica combinada com resposta rápida transistorizada", ampDefaults("Bass", gain = 36f, bass = 64f)),
        CatalogItem(117, "117PjbCub_BS", "Bass", "Phil Jones Bass Cub 100 • Ultra-portátil de alta fidelidade para ensaios acústicos", ampDefaults("Bass", gain = 28f)),
        CatalogItem(118, "118Tc21Vt_BS", "Bass", "Tech21 VT Bass 500 • Overdrive de baixo simulando válvulas com elasticidade para vintage rock", ampDefaults("Bass", gain = 44f, bass = 64f, middle = 60f)),
        CatalogItem(119, "119WatMod_BS", "Bass", "Hiwatt DR103 Mod • Contrabaixo britânico vintage de headroom colossal, firme e limpo", ampDefaults("Bass", gain = 32f, bass = 64f, reso = 70f)),
        CatalogItem(120, "120GKL800_BS", "Bass", "Gallien Krueger 800RB • Amplificador de baixo dos anos 80/90 com médios cortantes e ataque rápido", ampDefaults("Bass", gain = 38f, middle = 64f, treble = 62f))
    )

    val CAB_MODELS = listOf(
        CatalogItem(1, "1BogUber412A", "4x12", "Bogner Ubercab 412 • Shure SM57 (Dinâmico clássico)", cabDefaults("4x12")),
        CatalogItem(2, "2BogUber412B", "4x12", "Bogner Ubercab 412 • Sennheiser MD421 (Dinâmico encorpado)", cabDefaults("4x12")),
        CatalogItem(3, "3BogUber412C", "4x12", "Bogner Ubercab 412 • Beyerdynamic M160 (Fita quente)", cabDefaults("4x12")),
        CatalogItem(4, "4BogUber412D", "4x12", "Bogner Ubercab 412 • Sennheiser MD421 (Posição alternativa)", cabDefaults("4x12")),
        CatalogItem(5, "5DizFL412A", "4x12", "Diezel Front Loaded 412 • Shure SM57 (Ataque rápido)", cabDefaults("4x12")),
        CatalogItem(6, "6DizFL412B", "4x12", "Diezel Front Loaded 412 • Sennheiser MD421 (Graves secos)", cabDefaults("4x12")),
        CatalogItem(7, "7DizFL412C", "4x12", "Diezel Front Loaded 412 • Beyerdynamic M160 (Médios suaves)", cabDefaults("4x12")),
        CatalogItem(8, "8EngXXL412", "4x12", "ENGL V30 412 XXL • Sennheiser MD421 (Punch pesado)", cabDefaults("4x12")),
        CatalogItem(9, "9EvStright412", "4x12", "EVH 5150 Straight 412 • Neumann U87 (Condensador Hi-End)", cabDefaults("4x12")),
        CatalogItem(10, "10FdTR65R212A", "2x12", "Fender Twin Reverb 65 Reissue • Neumann U87 (Limpo cristalino)", cabDefaults("2x12")),
        CatalogItem(11, "11FdTR65R212B", "2x12", "Fender Twin Reverb 65 Reissue • Shure SM57 (Estalado clássico)", cabDefaults("2x12")),
        CatalogItem(12, "12FdBmanTw410", "4x10", "Fender Bassman Tweed 410 • Shure SM57 (Médios vintage)", cabDefaults("4x10")),
        CatalogItem(13, "13FdDelux112A", "2x12", "Fender Deluxe 112/212 • Shure SM57 (Dinâmica de blues)", cabDefaults("2x12")),
        CatalogItem(14, "14FdDelux112B", "1x12", "Fender Deluxe 112 • Sennheiser MD421 (Caloroso)", cabDefaults("1x12")),
        CatalogItem(15, "15FdDBface112", "1x12", "Fender Deluxe Blackface 112 • Neumann U87 (Palco sonoro aberto)", cabDefaults("1x12")),
        CatalogItem(16, "16FdDTweed112", "1x12", "Fender Deluxe Tweed 112 • Shure SM57 (Compressão de cone)", cabDefaults("1x12")),
        CatalogItem(17, "17FdPrinc110A", "1x10", "Fender Princeton 110 • Shure SM57 (Gravação compacta)", cabDefaults("1x10")),
        CatalogItem(18, "18FdPrinc110B", "1x10", "Fender Princeton 110 • Neumann U87 (Rico em harmônicos)", cabDefaults("1x10")),
        CatalogItem(19, "19FdTremol210", "2x10", "Fender Tremolux 210 • Sennheiser MD421 (Articulação ágil)", cabDefaults("2x10")),
        CatalogItem(20, "20FdTwRev212A", "2x12", "Fender Twin Reverb 212 • Shure SM57 (Padrão de palco)", cabDefaults("2x12")),
        CatalogItem(21, "21FdTwRev212B", "2x12", "Fender Twin Reverb 212 • Neumann U87 (Equilíbrio estéreo)", cabDefaults("2x12")),
        CatalogItem(22, "22Fman212A", "2x12", "Friedman 212 Vintage • Shure SM57 (Focado em rock pesado)", cabDefaults("2x12")),
        CatalogItem(23, "23Fman212B", "2x12", "Friedman 212 Vintage • Sennheiser MD421 (Graves estendidos)", cabDefaults("2x12")),
        CatalogItem(24, "24GojaCabA", "Custom", "GojaCab Custom Metal A • Multi-Mic (Timbre pesado para Djent)", cabDefaults("Custom")),
        CatalogItem(25, "25GojaCabB", "Custom", "GojaCab Custom Metal B • Multi-Mic (Corte agressivo na mix)", cabDefaults("Custom")),
        CatalogItem(26, "26HesModn212A", "2x12", "Hesu Modern M212 Demon • Shure SM57 (Modern Metalcore)", cabDefaults("2x12")),
        CatalogItem(27, "27HesModn212B", "2x12", "Hesu Modern M212 Demon • Neumann U87 (Alta definição e punch)", cabDefaults("2x12")),
        CatalogItem(28, "28HiwSE412A", "4x12", "Hiwatt SE4123F (Fane) • Sennheiser MD421 (Clean britânico imponente)", cabDefaults("4x12")),
        CatalogItem(29, "29HiwSE412B", "4x12", "Hiwatt SE4123F (Fane) • Shure SM57 (Médios penetrantes)", cabDefaults("4x12")),
        CatalogItem(30, "30HiwSE412C", "4x12", "Hiwatt SE4123F (Fane) • Beyerdynamic M160 (Clássico The Who / Gilmour)", cabDefaults("4x12")),
        CatalogItem(31, "31HughKe4x12A", "4x12", "Hughes & Kettner TC 412 • Shure SM57 (Alta resposta dinâmica)", cabDefaults("4x12")),
        CatalogItem(32, "32HughKe4x12B", "4x12", "Hughes & Kettner TC 412 • Sennheiser MD421 (Graves articulados)", cabDefaults("4x12")),
        CatalogItem(33, "33Mar60A412A", "4x12", "Marshall 1960A Slanted (G12T75) • Sennheiser MD421 (Padrão Hard Rock)", cabDefaults("4x12")),
        CatalogItem(34, "34Mar60A412B", "4x12", "Marshall 1960A Slanted (G12T75) • Sennheiser MD421 (Angulação alternativa)", cabDefaults("4x12")),
        CatalogItem(35, "35Mar60A412C", "4x12", "Marshall 1960A Slanted (G12T75) • Beyerdynamic M160 (Sedoso e encorpado)", cabDefaults("4x12")),
        CatalogItem(36, "36Mar60B412A", "4x12", "Marshall 1960B Straight • Neumann U87 (Graves profundos firmes)", cabDefaults("4x12")),
        CatalogItem(37, "37Mar60B412B", "4x12", "Marshall 1960B Straight • Shure SM57 (Mordida clássica)", cabDefaults("4x12")),
        CatalogItem(38, "38Mar60B412C", "4x12", "Marshall 1960B Straight • Sennheiser MD421 (Equilibrado para bases)", cabDefaults("4x12")),
        CatalogItem(39, "39Mar35Bac412", "4x12", "Marshall 1935B Alnico Cream • Beyerdynamic M160 (Doçura vintage única)", cabDefaults("4x12")),
        CatalogItem(40, "40Mar60TV412", "4x12", "Marshall 1960 TV (Tall Vintage) • Shure SM57 (Ressonância ampla)", cabDefaults("4x12")),
        CatalogItem(41, "41Mar2551B412", "4x12", "Marshall 2551BV Silver Jubilee • Neumann U87 (Vintage 30 britânico puro)", cabDefaults("4x12")),
        CatalogItem(42, "42MatCSig212A", "2x12", "Matchless Chieftain 212 • Shure SM57 (Boutique chime definido)", cabDefaults("2x12")),
        CatalogItem(43, "43MatCSig212B", "2x12", "Matchless Chieftain 212 • Neumann U87 (Espacialidade e ar)", cabDefaults("2x12")),
        CatalogItem(44, "44Mat30DC212A", "2x12", "Matchless DC30 212 • Shure SM57 (Articulação de classe A)", cabDefaults("2x12")),
        CatalogItem(45, "45Mat30DC212B", "2x12", "Matchless DC30 212 • Sennheiser MD421 (Médios doces e densos)", cabDefaults("2x12")),
        CatalogItem(46, "46MbOS412A", "4x12", "Mesa Boogie Recto Oversized 412 • Shure SM57 (Timbre definitivo de Metal)", cabDefaults("4x12")),
        CatalogItem(47, "47MbOS412B", "4x12", "Mesa Boogie Recto Oversized 412 • Sennheiser MD421 (Graves gigantes e firmes)", cabDefaults("4x12")),
        CatalogItem(48, "48MbSD212A", "2x12", "Mesa Boogie Standard 212 • Shure SM57 (Compacto e versátil)", cabDefaults("2x12")),
        CatalogItem(49, "49MbSD212B", "2x12", "Mesa Boogie Standard 212 • Neumann U87 (Equilíbrio para estúdio)", cabDefaults("2x12")),
        CatalogItem(50, "50MbOsAngl412", "4x12", "Mesa Boogie Oversize Angled 2003 • Sennheiser MD421 (Referência Nu-Metal)", cabDefaults("4x12")),
        CatalogItem(51, "51MbRecLed212", "2x12", "Mesa Boogie Rectifier Legend 212 • Shure SM57 (Ataque cirúrgico)", cabDefaults("2x12")),
        CatalogItem(52, "52MbRecVin212", "2x12", "Mesa Boogie Rectifier 212 (V30) • Neumann U87 (Gravação rica em detalhes)", cabDefaults("2x12")),
        CatalogItem(53, "53MbSOSAn412", "4x12", "Mesa Boogie Standard OS Angled • Neumann U87 (Projeção angular massiva)", cabDefaults("4x12")),
        CatalogItem(54, "54MbSOSS4t412", "4x12", "Mesa Boogie Standard OS Straight • Shure SM57 (Graves de alta pressão)", cabDefaults("4x12")),
        CatalogItem(55, "55MbTradAn412", "4x12", "Mesa Boogie Traditional Angled • Sennheiser MD421 (Médios focados e rápidos)", cabDefaults("4x12")),
        CatalogItem(56, "56MbTradS412A", "4x12", "Mesa Boogie Traditional Straight • Shure SM57 (Resposta precisa de palheta)", cabDefaults("4x12")),
        CatalogItem(57, "57MbTradS412B", "4x12", "Mesa Boogie Traditional Straight • Sennheiser MD421 (Encorpado para bases)", cabDefaults("4x12")),
        CatalogItem(58, "58OgPPC412A", "4x12", "Orange PPC 412 (Madeira densa) • Shure SM57 (Médios encorpados britânicos)", cabDefaults("4x12")),
        CatalogItem(59, "59OgPPC412B", "4x12", "Orange PPC 412 (Madeira densa) • Sennheiser MD421 (Graves quentes de stoner rock)", cabDefaults("4x12")),
        CatalogItem(60, "60PinilCab", "Custom", "PinilCab Custom Studio • Multi-Mic (Otimização moderna)", cabDefaults("Custom")),
        CatalogItem(61, "61RJ120azz212", "2x12", "Roland JC-120 Factory 212 • Neumann U87 (Clean plano de alta precisão)", cabDefaults("2x12")),
        CatalogItem(62, "62SurCab212", "2x12", "Suhr PT15 Pete Thorn 212 • Shure SM57 (Equilíbrio boutique supremo)", cabDefaults("2x12")),
        CatalogItem(63, "63VAC15ox112", "1x12", "Vox AC15 112 (Celestion Blue) • Shure SM57 (Brilho harmônico doce)", cabDefaults("1x12")),
        CatalogItem(64, "64VAC30TBC212", "2x12", "Vox AC30 TopBoost 212 (Alnico) • Sennheiser MD421 (Chime clássico britânico)", cabDefaults("2x12")),
        CatalogItem(65, "65VAC30ox212", "2x12", "Vox AC30 212 (Celestion Blue) • Shure SM57 (Mordida cortante para solos)", cabDefaults("2x12")),
        CatalogItem(66, "66ZillCab212", "2x12", "Zilla Cabs Custom 212 • Shure SM57 (Favorito de gravações modernas)", cabDefaults("2x12")),
        CatalogItem(67, "67ZillCust412", "4x12", "Zilla Custom 412 • Sennheiser MD421 (Potência de turnê)", cabDefaults("4x12")),
        CatalogItem(68, "68ZillFboy212", "2x12", "Zilla Fatboy 2002 212 • Neumann U87 (Caixa profunda super encorpada)", cabDefaults("2x12")),
        CatalogItem(69, "69ZillMiMo112", "1x12", "Zilla Mini Modern 112 • Shure SM57 (Foco e agilidade de estúdio)", cabDefaults("1x12")),
        CatalogItem(70, "70ZillOpen212", "2x12", "Zilla Open Back 212 • Beyerdynamic M160 (Traseira aberta surround)", cabDefaults("2x12")),
        CatalogItem(71, "71GisonHubird", "Acoustic", "Gibson Hummingbird Dreadnought • Neumann U87 (Simulação acústica quente)", cabDefaults("Acoustic")),
        CatalogItem(72, "72LakewdDCP", "Acoustic", "Lakewood D-18CP Grand Concert • Neumann U87 (Clareza de fingerstyle europeu)", cabDefaults("Acoustic")),
        CatalogItem(73, "73TakmiENPU", "Acoustic", "Takamine EN10 Electro-Acoustic • Neumann U87 (Resposta percussiva para palco)", cabDefaults("Acoustic")),
        CatalogItem(74, "74TaylrOcE", "Acoustic", "Taylor CE414 Grand Auditorium • Shure SM57 (Brilho moderno articulado)", cabDefaults("Acoustic")),
        CatalogItem(75, "75YamhaFGX", "Acoustic", "Yamaha FGX412 Folk Acoustic • Shure SM57 (Corpo sólido e presença acústica)", cabDefaults("Acoustic")),
        CatalogItem(76, "76Acous215", "Bass 2x15", "Acoustic 215 Vintage Bass Cab • Sennheiser MD421 (Subgraves massivos clássicos)", cabDefaults("Bass 2x15")),
        CatalogItem(77, "77AmpgSVT115", "Bass 1x15", "Ampeg SVT-115E Bass Enclosure • Neumann U87 (Calibre aveludado e fundo musical)", cabDefaults("Bass 1x15")),
        CatalogItem(78, "78AmpgSVT410", "Bass 4x10", "Ampeg SVT-410HLF (com corneta) • Sennheiser MD421 (Punch rápido e estalo na mix)", cabDefaults("Bass 4x10")),
        CatalogItem(79, "79AmpgSVT412", "Bass 4x12", "Ampeg SVT-412HE • Neumann U87 (Médios viscerais de rock n' roll)", cabDefaults("Bass 4x12")),
        CatalogItem(80, "80AmpgSVT810", "Bass 8x10", "Ampeg SVT-810E (A famosa Geladeira) • Sennheiser MD421 (Lenda de palcos)", cabDefaults("Bass 8x10")),
        CatalogItem(81, "81AshdowRM212", "Bass 2x12", "Ashdown Rootmaster EVO II 212 • Beyerdynamic M160 (Médios britânicos redondos)", cabDefaults("Bass 2x12")),
        CatalogItem(82, "82Bareface110", "Bass 1x10", "Barefaced One 10T 110 • Sennheiser MD421 (Hi-Fi ultraleve boutique)", cabDefaults("Bass 1x10")),
        CatalogItem(83, "83DgD210C210", "Bass 2x10", "Darkglass DG210C 210 • Shure SM57 (Corte de distorção moderna)", cabDefaults("Bass 2x10")),
        CatalogItem(84, "84DgDG212N212", "Bass 2x12", "Darkglass DG212N Neodymium 212 • Sennheiser MD421 (Graves rápidos para Drop)", cabDefaults("Bass 2x12")),
        CatalogItem(85, "85DavEendD410", "Bass 4x10", "David Eden D410XLT • Neumann U87 (Agudos cristalinos para slap)", cabDefaults("Bass 4x10")),
        CatalogItem(86, "86FdBmanSf210", "Bass 2x10", "Fender Bassman Silverface 210 • Shure SM57 (Vintage soul e motown)", cabDefaults("Bass 2x10")),
        CatalogItem(87, "87FdBmanNo410", "Bass 4x10", "Fender Bassman 410 PRO • Sennheiser MD421 (Firmeza equilibrada)", cabDefaults("Bass 4x10")),
        CatalogItem(88, "88FdRumble210", "Bass 2x10", "Fender Rumble 210 V3 • Shure SM57 (Resposta linear para pop/rock)", cabDefaults("Bass 2x10")),
        CatalogItem(89, "89GK410RB410A", "Bass 4x10", "Gallien-Krueger 410RBH (Horn ativo) • Shure SM57 (Clássico ataque funk/rock)", cabDefaults("Bass 4x10")),
        CatalogItem(90, "90GK410RB410B", "Bass 4x10", "Gallien-Krueger 410RBH • Neumann U87 (Graves cheios e naturais)", cabDefaults("Bass 4x10")),
        CatalogItem(91, "91Har410xl410", "Bass 4x10", "Hartke 410XL (Cone de Alumínio) • Sennheiser MD421 (Ataque metálico inconfundível)", cabDefaults("Bass 4x10")),
        CatalogItem(92, "92Mar1935A412", "Bass 4x12", "Marshall 1935A Bass 412 • Shure SM57 (Rock setentista e hard rock)", cabDefaults("Bass 4x12")),
        CatalogItem(93, "93MbSubway210", "Bass 2x10", "Mesa Boogie Subway Ultra-Lite 210 • Sennheiser MD421 (Potente e veloz de neodímio)", cabDefaults("Bass 2x10")),
        CatalogItem(94, "94MbPhouse210", "Bass 2x10", "Mesa Boogie PowerHouse 210 • Neumann U87 (Paredão sonoro maciço)", cabDefaults("Bass 2x10")),
        CatalogItem(95, "95OgV30PPC212", "Bass 2x12", "Orange PPC 212 Bass/Guitar Hybrid • Shure SM57 (Distorção de baixo pesada)", cabDefaults("Bass 2x12")),
        CatalogItem(96, "96Pey115BX115", "Bass 1x15", "Peavey 115BX Black Widow 115 • Shure SM57 (Clássico indestrutível de palco)", cabDefaults("Bass 1x15")),
        CatalogItem(97, "97Sun19L215", "Bass 2x15", "Sunn Vintage 1970s 215 (JBL) • Sennheiser MD421 (Doom, fuzz e vibração física)", cabDefaults("Bass 2x15")),
        CatalogItem(98, "98SWorkman410", "Bass 4x10", "SWR Workingman 410 • Shure SM57 (Médios abertos e clareza para slap)", cabDefaults("Bass 4x10")),
        CatalogItem(99, "99TaceEll412", "Bass 4x12", "Trace Elliot 412 Bass Cab • Sennheiser MD421 (Definição rápida britânica)", cabDefaults("Bass 4x12")),
        CatalogItem(100, "100TaceEll118", "Bass 1x18", "Trace Elliot 1818T 118 Subwoofer • Neumann U87 (Subgraves monumentais de 18\")", cabDefaults("Bass 1x18"))
    )

    private fun ds8Defaults(): Map<String, Float> = mapOf(
        "gain" to 50f,
        "level" to 50f,
        "bass" to 50f,
        "middle" to 50f,
        "treble" to 50f,
        "reso" to 0f,
        "pres" to 50f,
        "bright" to 100f
    )

    val DS_MODELS = listOf(
        CatalogItem(1, "MW-808", "Tube Screamer TS-808", "gain=000, tone=050, level=100 — Emulação fiel do icônico Ibanez TS808 com médios focados", mapOf("gain" to 0f, "tone" to 50f, "level" to 100f)),
        CatalogItem(2, "MW-GOLD", "Klon Centaur", "gain=050, tone=050, level=70 — Overdrive transparente de boutique com articulação de acordes", mapOf("gain" to 50f, "tone" to 50f, "level" to 70f)),
        CatalogItem(3, "MW-OCD", "Fulltone OCD", "gain=000, tone=050, LPtype=off, mode=2/2, level=100 — Overdrive/Distortion dinâmico touch-sensitive", mapOf("gain" to 0f, "tone" to 50f, "LPtype" to 0f, "mode" to 2f, "level" to 100f)),
        CatalogItem(4, "MW-SD1", "Super Overdrive", "gain=000, tone=050, level=100 — Inspirado no lendário Boss SD-1 com médios destacados", mapOf("gain" to 0f, "tone" to 50f, "level" to 100f)),
        CatalogItem(5, "MW-DS1", "Distortion Clássica", "gain=000, tone=050, level=100 — Emulação do clássico Boss DS-1 áspero, agressivo e cortante", mapOf("gain" to 0f, "tone" to 50f, "level" to 100f)),
        CatalogItem(6, "MW-RAT", "Distorção Encorpada", "gain=000, filter=050, level=100 — Inspirado no ProCo Rat com caráter fuzzy e médios densos", mapOf("gain" to 0f, "filter" to 50f, "level" to 100f)),
        CatalogItem(7, "MW-XEP", "Xotic EP Booster", "gain=000, boost=off, bright=off — Emulação do pré Echoplex com graves encorpados e brilho", mapOf("gain" to 0f, "boost" to 0f, "bright" to 0f)),
        CatalogItem(8, "MW-FACE", "Fuzz Face Vintage", "fuzz=090, level=100 — Fuzz clássico de silício/germânico orgânico e dinâmico", mapOf("fuzz" to 90f, "level" to 100f)),
        CatalogItem(9, "MW-HRZ", "High-Gain com Gate", "gain=020, attack=1/6, bright=100, gate=000, gate mode=1/2, level=100 — Distorção high-gain com Gate integrado", mapOf("gain" to 20f, "attack" to 1f, "bright" to 100f, "gate" to 0f, "gate mode" to 1f, "level" to 100f)),
        CatalogItem(10, "MW-EQP", "Overdrive Paramétrico", "gain=000, tone=050, level=100, mode=1/3 — Overdrive flexível com 3 modos de corte", mapOf("gain" to 0f, "tone" to 50f, "level" to 100f, "mode" to 1f)),
        CatalogItem(11, "MW-MUFF", "Big Muff Pi Fuzz", "sustain=080, tone=050, level=100 — Fuzz encorpado com sustain massivo e textura espessa", mapOf("sustain" to 80f, "tone" to 50f, "level" to 100f)),
        CatalogItem(12, "MW-B7000", "Darkglass B7K Preamp", "gain=050, level=050, blend=050, grunt=2/3, attack=2/3, master=050, bass=050, loMidFreq=2/3, loMid=050, hiMidFreq=2/3, hiMid=050, treble=050 — Pré de baixo de 12 parâmetros", mapOf("gain" to 50f, "level" to 50f, "blend" to 50f, "grunt" to 2f, "attack" to 2f, "master" to 50f, "bass" to 50f, "loMidFreq" to 2f, "loMid" to 50f, "hiMidFreq" to 2f, "hiMid" to 50f, "treble" to 50f)),
        CatalogItem(13, "1._ADAM_RAFOW", "Adam Rafow High-Gain", "gain=050, level=050, bass=050, middle=050, treble=050, reso=000, pres=050, bright=100 — Stompbox high-gain", ds8Defaults()),
        CatalogItem(14, "2TS8", "Tube Screamer Mod v2", "gain=050, level=050, bass=050, middle=050, treble=050, reso=000, pres=050, bright=100 — TS808 com EQ de 8 parâmetros", ds8Defaults()),
        CatalogItem(15, "3DS1", "DS-1 Mod Alto Ganho", "gain=050, level=050, bass=050, middle=050, treble=050, reso=000, pres=050, bright=100 — DS-1 Mod com EQ de 8 parâmetros", ds8Defaults()),
        CatalogItem(16, "4DS2", "DS-2 Turbo Emulation", "gain=050, level=050, bass=050, middle=050, treble=050, reso=000, pres=050, bright=100 — Boss DS-2 Turbo Distortion", ds8Defaults()),
        CatalogItem(17, "5M-VAVE_OD", "5M-VAVE Custom OD", "gain=050, level=050, bass=050, middle=050, treble=050, reso=000, pres=050, bright=100 — Overdrive customizado M-VAVE", ds8Defaults()),
        CatalogItem(18, "6M-VAVE_DS", "6M-VAVE Custom DS", "gain=050, level=050, bass=050, middle=050, treble=050, reso=000, pres=050, bright=100 — Distorção customizada M-VAVE", ds8Defaults()),
        CatalogItem(19, "7M-VAVE_TS1", "Custom TS1 Overdrive", "gain=050, level=050, bass=050, middle=050, treble=050, reso=000, pres=050, bright=100 — Circuito customizado de TS v1", ds8Defaults()),
        CatalogItem(20, "8M-VAVE_TS2", "Custom TS2 Overdrive", "gain=050, level=050, bass=050, middle=050, treble=050, reso=000, pres=050, bright=100 — Circuito customizado de TS v2", ds8Defaults()),
        CatalogItem(21, "9SUPA_1", "Super Overdrive v1", "gain=050, level=050, bass=050, middle=050, treble=050, reso=000, pres=050, bright=100 — Circuito de overdrive encorpado v1", ds8Defaults()),
        CatalogItem(22, "10SUPA_2", "Super Overdrive v2", "gain=050, level=050, bass=050, middle=050, treble=050, reso=000, pres=050, bright=100 — Circuito de overdrive encorpado v2", ds8Defaults()),
        CatalogItem(23, "11RAT", "ProCo Rat Custom", "gain=050, level=050, bass=050, middle=050, treble=050, reso=000, pres=050, bright=100 — Emulação aprimorada do Rat", ds8Defaults()),
        CatalogItem(24, "12RAT_BT", "ProCo Rat Fat/Turbo", "gain=050, level=050, bass=050, middle=050, treble=050, reso=000, pres=050, bright=100 — Modificação do Rat Fat/Turbo", ds8Defaults()),
        CatalogItem(25, "13JHS_1", "JHS Modded OD v1", "gain=050, level=050, bass=050, middle=050, treble=050, reso=000, pres=050, bright=100 — Overdrive boutique JHS v1", ds8Defaults()),
        CatalogItem(26, "14JHS_2", "JHS Modded OD v2", "gain=050, level=050, bass=050, middle=050, treble=050, reso=000, pres=050, bright=100 — Overdrive boutique JHS v2", ds8Defaults()),
        CatalogItem(27, "15MT_1", "Metal Distortion v1", "gain=050, level=050, bass=050, middle=050, treble=050, reso=000, pres=050, bright=100 — Distorção high-gain metal v1", ds8Defaults()),
        CatalogItem(28, "16MT_2", "Metal Distortion v2", "gain=050, level=050, bass=050, middle=050, treble=050, reso=000, pres=050, bright=100 — Distorção high-gain metal v2", ds8Defaults()),
        CatalogItem(29, "17TDS", "TDS High-Gain", "gain=050, level=050, bass=050, middle=050, treble=050, reso=000, pres=050, bright=100 — Distorção moderna TDS", ds8Defaults()),
        CatalogItem(30, "18XC_DS", "18X Custom Distortion", "gain=050, level=050, bass=050, middle=050, treble=050, reso=000, pres=050, bright=100 — Distorção boutique de alta definição", ds8Defaults()),
        CatalogItem(31, "19QC_DS", "QC Custom Distortion", "gain=050, level=050, bass=050, middle=050, treble=050, reso=000, pres=050, bright=100 — Distorção customizada QC", ds8Defaults()),
        CatalogItem(32, "20HIGAIN", "High-Gain Monster", "gain=050, level=050, bass=050, middle=050, treble=050, reso=000, pres=050, bright=100 — Distorção extrema para solos e metal", ds8Defaults()),
        CatalogItem(33, "21M-BOOSTER", "M-Booster High-Headroom", "gain=050, level=050, bass=050, middle=050, treble=050, reso=000, pres=050, bright=100 — Booster de alta margem dinâmica", ds8Defaults()),
        CatalogItem(34, "22TS-9", "Ibanez TS9 Tube Screamer", "gain=050, level=050, bass=050, middle=050, treble=050, reso=000, pres=050, bright=100 — Emulação clássica do Ibanez TS9", ds8Defaults()),
        CatalogItem(35, "23BIG-DR", "Big Drive Distortion", "gain=050, level=050, bass=050, middle=050, treble=050, reso=000, pres=050, bright=100 — Distorção robusta Big Drive", ds8Defaults()),
        CatalogItem(36, "24CL_BOOST", "Clean Booster Transparent", "gain=050, level=050, bass=050, middle=050, treble=050, reso=000, pres=050, bright=100 — Boost limpo linear", ds8Defaults()),
        CatalogItem(37, "25BD", "Bass Driver DI Overdrive", "gain=050, level=050, bass=050, middle=050, treble=050, reso=000, pres=050, bright=100 — Emulação de pré/overdrive BD", ds8Defaults()),
        CatalogItem(38, "26M90S", "M90S Modern Overdrive", "gain=050, level=050, bass=050, middle=050, treble=050, reso=000, pres=050, bright=100 — Overdrive contemporâneo M90S", ds8Defaults()),
        CatalogItem(39, "27M2000", "27M2000 Distortion", "gain=050, level=050, bass=050, middle=050, treble=050, reso=000, pres=050, bright=100 — Distorção 27M2000", ds8Defaults()),
        CatalogItem(40, "28DS800", "DS-800 High-Output", "gain=050, level=050, bass=050, middle=050, treble=050, reso=000, pres=050, bright=100 — Stompbox de alta saída DS800", ds8Defaults()),
        CatalogItem(41, "29DS900", "DS-900 Heavy Metal", "gain=050, level=050, bass=050, middle=050, treble=050, reso=000, pres=050, bright=100 — Distorção pesada DS900", ds8Defaults()),
        CatalogItem(42, "30MAR-DS", "Marshall-Style Distortion", "gain=050, level=050, bass=050, middle=050, treble=050, reso=000, pres=050, bright=100 — Distorção britânica estilo Marshall", ds8Defaults()),
        CatalogItem(43, "31BOG_DS", "Bogu-Style High Gain", "gain=050, level=050, bass=050, middle=050, treble=050, reso=000, pres=050, bright=100 — Distorção valvulada estilo Bogner", ds8Defaults()),
        CatalogItem(44, "32SONDO", "Sondo Custom Distortion", "gain=050, level=050, bass=050, middle=050, treble=050, reso=000, pres=050, bright=100 — Distorção customizada Sondo", ds8Defaults()),
        CatalogItem(45, "33MID-BOST", "Mid-Boost Lead Pedal", "gain=050, level=050, bass=050, middle=050, treble=050, reso=000, pres=050, bright=100 — Pedal de destaque de médios para solos", ds8Defaults()),
        CatalogItem(46, "34RED_DS", "Red Distortion Stomp", "gain=050, level=050, bass=050, middle=050, treble=050, reso=000, pres=050, bright=100 — Distorção vermelha de alta voltagem", ds8Defaults()),
        CatalogItem(47, "35MODEN_DS", "Modern High-Gain", "gain=050, level=050, bass=050, middle=050, treble=050, reso=000, pres=050, bright=100 — Distorção moderna cristalina", ds8Defaults()),
        CatalogItem(48, "36SUPEROD", "Super Overdrive Custom", "gain=050, level=050, bass=050, middle=050, treble=050, reso=000, pres=050, bright=100 — Circuito otimizado Super Overdrive", ds8Defaults()),
        CatalogItem(49, "37BLUES_DR", "Blues Driver Dinâmico", "gain=050, level=050, bass=050, middle=050, treble=050, reso=000, pres=050, bright=100 — Overdrive sensível ao toque Blues Driver", ds8Defaults()),
        CatalogItem(50, "38BLACK-BOX", "Black Box Distortion", "gain=050, level=050, bass=050, middle=050, treble=050, reso=000, pres=050, bright=100 — Distorção boutique Black Box", ds8Defaults()),
        CatalogItem(51, "39BIG-MUFF", "Big Muff Pi Fuzz Sustaining", "gain=050, level=050, bass=050, middle=050, treble=050, reso=000, pres=050, bright=100 — Fuzz Big Muff com EQ de 8 parâmetros", ds8Defaults()),
        CatalogItem(52, "SLASH_USE_YOU", "Slash Hard Rock Tone", "gain=050, level=050, bass=050, middle=050, treble=050, reso=000, pres=050, bright=100 — Modelagem dedicada Slash / Guns N' Roses", ds8Defaults())
    )

    // Ordem oficial exata de hardware da MK-20 (1..20 -> byte 0..19 em 0x32)
    val FX_MODELS = listOf(
        CatalogItem(1, "Wah-Wah", "AUTO / LFO WAH", "Speed (0.1..10.0), Q (0..100), Mix (0..100), Width (0..100), Level (0..100), Sync, SyncBpm (40..240) — Auto-Wah cíclico por LFO", mapOf("Speed" to 2.5f, "Q" to 50f, "Mix" to 80f, "Width" to 25f, "Level" to 50f, "Sync" to 0f, "SyncBpm" to 80f)),
        CatalogItem(2, "Lofi", "BITCRUSHER", "Bit (2..24), Level (0..100), Filter (0..100) — Degradação vintage com redução de bits e filtro", mapOf("Bit" to 12f, "Level" to 50f, "Filter" to 50f)),
        CatalogItem(3, "Sense-Wah", "ENVELOPE FILTER", "Sense (0..10.0), Attack, Q, fPeak, Mix, Width, Level — Wah dinâmico sensível ao ataque da palhetada", mapOf("Sense" to 5.0f, "Attack" to 40f, "Q" to 60f, "fPeak" to 50f, "Mix" to 60f, "Width" to 65f, "Level" to 70f)),
        CatalogItem(4, "Boost", "CLEAN BOOST", "Gain (0..10.0), Level (0..100) — Elevador limpo de ganho e volume puro", mapOf("Gain" to 4.5f, "Level" to 75f)),
        CatalogItem(5, "A Boost", "TYPE A HIGH BOOST", "Gain (0..10.0), Bass, Mid, Treble, Level — Pré-amp booster tipo A com foco em agudos e presença", mapOf("Gain" to 4.0f, "Bass" to 50f, "Mid" to 55f, "Treble" to 55f, "Level" to 70f)),
        CatalogItem(6, "E Boost", "TYPE E MID BOOST", "Gain (0..10.0), Bass, Mid, Treble, Level — Pré-amp booster tipo E com foco em médios", mapOf("Gain" to 5.0f, "Bass" to 50f, "Mid" to 65f, "Treble" to 55f, "Level" to 70f)),
        CatalogItem(7, "B Boost", "TYPE B LOW BOOST", "Gain (0..10.0), Bass, Mid, Treble, Level — Pré-amp booster tipo B encorpado estilo BB Preamp", mapOf("Gain" to 5.0f, "Bass" to 58f, "Mid" to 52f, "Treble" to 50f, "Level" to 70f)),
        CatalogItem(8, "Boost ED", "TYPE ED GAIN BOOST", "Gain (0..10.0), Grit (0..100), Level (0..100) — Booster com saturação harmônica Grit", mapOf("Gain" to 4.5f, "Grit" to 35f, "Level" to 72f)),
        CatalogItem(9, "Compress", "COMPRESSOR", "Sustain (0..10.0), Attack (0..100), Level (0..100), Blend (0..100) — Compressor clássico para nivelamento e sustain", mapOf("Sustain" to 5.0f, "Attack" to 50f, "Level" to 70f, "Blend" to 60f)),
        CatalogItem(10, "Compress Pro", "STUDIO COMP", "Ratio (0..10.0), Gain, Knee, Thd, Attack, Level, Blend — Compressor de estúdio de 7 parâmetros", mapOf("Ratio" to 4.0f, "Gain" to 50f, "Knee" to 30f, "Thd" to 50f, "Attack" to 40f, "Level" to 70f, "Blend" to 80f)),
        CatalogItem(11, "F Compress", "OPTICAL / TONE COMP", "Ratio (0..10.0), Gain, Knee, Thd, Attack, Tone, Level, Blend — Compressor óptico de 8 parâmetros com Tone", mapOf("Ratio" to 3.5f, "Gain" to 50f, "Knee" to 30f, "Thd" to 50f, "Attack" to 40f, "Tone" to 60f, "Level" to 70f, "Blend" to 75f)),
        CatalogItem(12, "Pitch", "DUAL HARMONIZER", "High Pitch (0..24), Low Pitch (-24..0), High Level, Low Level, Dry Level — Harmonizador duplo", mapOf("High Pitch" to 12f, "Low Pitch" to -12f, "High Level" to 50f, "Low Level" to 50f, "Dry Level" to 80f)),
        CatalogItem(13, "Pitch shifter", "CHROMATIC SHIFT", "Semi (-12..12) — Transpositor de afinação em semitons", mapOf("Semi" to 0f)),
        CatalogItem(14, "Octave", "CLASSIC OCTAVER", "High Level (0..100), Low Level (0..100), Dry Level (0..100) — Gerador de oitava", mapOf("High Level" to 40f, "Low Level" to 60f, "Dry Level" to 80f)),
        CatalogItem(15, "Ring", "RING MODULATOR", "Freq (0..100), Mix (0..100) — Modulador em anel metálico", mapOf("Freq" to 45f, "Mix" to 50f)),
        CatalogItem(16, "X-Wah", "MODERN WAH", "Value (0..100), Gain (0..100), Level (0..100) — Wah transparente de graves a médios", mapOf("Value" to 50f, "Gain" to 55f, "Level" to 70f)),
        CatalogItem(17, "Funk-Wah", "HIGH-FREQ WAH", "Value (0..100), Gain (0..100), Level (0..100) — Wah brilhante focado em agudos e funk", mapOf("Value" to 50f, "Gain" to 55f, "Level" to 70f)),
        CatalogItem(18, "Slide-Wah", "HIGH-GAIN WAH", "Value (0..100), Gain (0..100), Level (0..100) — Wah penetrante para distorção", mapOf("Value" to 50f, "Gain" to 50f, "Level" to 70f)),
        CatalogItem(19, "Cry-Wah", "CLASSIC WAH", "Value (0..100), Gain (0..100), Level (0..100) — Wah clássico vocal estilo Cry Baby", mapOf("Value" to 50f, "Gain" to 50f, "Level" to 70f)),
        CatalogItem(20, "Whammy", "PITCH BEND EXP", "mode (1=Mbody, 2=Oct1, 3=Oct2), bend (0..100), Mix (0..100) — Pedal Pitch Whammy", mapOf("mode" to 2f, "bend" to 0f, "Mix" to 100f))
    )

    val NS_MODELS = listOf(
        CatalogItem(1, "ai gate", "ADAPTIVE NEURAL", "gate (0..100), bias (0..100), inputDetect (0=Input, 1=PreGate) — Noise Gate IA", mapOf("gate" to 50f, "bias" to 50f, "inputDetect" to 0f)),
        CatalogItem(2, "ai ms gate", "FAST STACCATO", "gate (0..100), bias (0..100), inputDetect (0=Input, 1=PreGate) — Gate IA Gen2 ultra-rápido", mapOf("gate" to 50f, "bias" to 50f, "inputDetect" to 0f)),
        CatalogItem(3, "soft gate", "SMOOTH DECAY", "thd (-80..-30), inputDetect (0=Input, 1=PreGate) — Noise gate suave", mapOf("thd" to -60f, "inputDetect" to 0f)),
        CatalogItem(4, "hard gate", "HIGH-GAIN CUT", "thd (-80..-30), inputDetect (0=Input, 1=PreGate) — Corte rigoroso de ruído", mapOf("thd" to -60f, "inputDetect" to 0f)),
        CatalogItem(5, "pro gate", "STUDIO MULTI-PARAMETRIC", "att, rel, thd (-80..-30), kw, ratio, inputDetect — Noise gate multiparamétrico", mapOf("att" to 50f, "rel" to 50f, "thd" to -55f, "kw" to 50f, "ratio" to 50f, "inputDetect" to 0f)),
        CatalogItem(6, "compress", "STOMP COMP", "sustain, attack, level, blend, inputDetect — Compressor no bloco NS", mapOf("sustain" to 50f, "attack" to 50f, "level" to 50f, "blend" to 100f, "inputDetect" to 0f)),
        CatalogItem(7, "compress pro", "STUDIO COMP", "ratio, gain, knee, thd, attack, level, blend — Compressor Pro de 7 parâmetros no bloco NS", mapOf("ratio" to 50f, "gain" to 50f, "knee" to 50f, "thd" to 50f, "attack" to 50f, "level" to 50f, "blend" to 100f)),
        CatalogItem(8, "f compress", "OPTICAL / TONE COMP", "ratio, gain, knee, thd, attack, tone, level, blend, inputDetect — Compressor F de 9 parâmetros no bloco NS", mapOf("ratio" to 50f, "gain" to 50f, "knee" to 50f, "thd" to 50f, "attack" to 50f, "tone" to 60f, "level" to 50f, "blend" to 100f, "inputDetect" to 0f))
    )

    val EQ_MODELS = listOf(
        CatalogItem(
            1,
            "Guitar EQ 6",
            "6 Bandas (-12 a +12 dB)",
            "Equalizador de 6 bandas para guitarra (100Hz a 3.2kHz, Min=-12.0, Max=+12.0 dB)",
            mapOf("100Hz" to 0f, "200Hz" to 0f, "400Hz" to 0f, "800Hz" to 0f, "1.6kHz" to 0f, "3.2kHz" to 0f)
        ),
        CatalogItem(
            2,
            "Bass EQ 7",
            "7 Bandas (-12 a +12 dB)",
            "Equalizador de 7 bandas para contrabaixo (50Hz a 10k, Min=-12.0, Max=+12.0 dB)",
            mapOf("50Hz" to 0f, "120Hz" to 0f, "400Hz" to 0f, "500Hz" to 0f, "800Hz" to 0f, "4.5kHz" to 0f, "10k" to 0f)
        ),
        CatalogItem(
            3,
            "Normal EQ 10",
            "10 Bandas (-12 a +12 dB)",
            "Equalizador gráfico de 10 bandas (31.25Hz a 16kHz, Min=-12.0, Max=+12.0 dB)",
            mapOf("31.25Hz" to 0f, "62.5Hz" to 0f, "125Hz" to 0f, "250Hz" to 0f, "500Hz" to 0f, "1kHz" to 0f, "2kHz" to 0f, "4kHz" to 0f, "8kHz" to 0f, "16kHz" to 0f)
        )
    )

    val MOD_MODELS = listOf(
        CatalogItem(1, "CHORUS", "Espacial", "speed (0.1..10.0), depth (0..100), mix (0..100), sync, syncBpm (40..240) — Chorus clássico", mapOf("speed" to 1.5f, "depth" to 45f, "mix" to 35f, "sync" to 0f, "syncBpm" to 80f)),
        CatalogItem(2, "TRI CHORUS", "Espacial", "speed (0.1..10.0), depth (0..100), mix (0..100), sync, syncBpm (40..240) — Triple Chorus tridimensional", mapOf("speed" to 1.2f, "depth" to 50f, "mix" to 40f, "sync" to 0f, "syncBpm" to 80f)),
        CatalogItem(3, "FLANGER", "Modulação", "speed (0.1..10.0), depth, fb, mix, sync, syncBpm — Flanger clássico", mapOf("speed" to 1.5f, "depth" to 50f, "fb" to 45f, "mix" to 40f, "sync" to 0f, "syncBpm" to 80f)),
        CatalogItem(4, "TRI FLANGER", "Modulação", "speed (0.1..10.0), depth, fb, mix, sync, syncBpm — Triple Flanger", mapOf("speed" to 1.5f, "depth" to 50f, "fb" to 45f, "mix" to 40f, "sync" to 0f, "syncBpm" to 80f)),
        CatalogItem(5, "TREMOLO", "Dinâmica", "speed (0.1..10.0), depth, level, sync, syncBpm — Tremolo clássico", mapOf("speed" to 3.0f, "depth" to 50f, "level" to 70f, "sync" to 0f, "syncBpm" to 80f)),
        CatalogItem(6, "TRI TREMOLO", "Dinâmica", "speed (0.1..10.0), depth, level, sync, syncBpm — Triple Tremolo", mapOf("speed" to 3.0f, "depth" to 50f, "level" to 70f, "sync" to 0f, "syncBpm" to 80f)),
        CatalogItem(7, "OPTO TREMOLO", "Dinâmica", "speed (0.1..10.0), depth, level, sync, syncBpm — Tremolo óptico vintage", mapOf("speed" to 3.0f, "depth" to 50f, "level" to 70f, "sync" to 0f, "syncBpm" to 80f)),
        CatalogItem(8, "PHASER", "Fase", "speed (0.1..10.0), midcut, reso, fb, sync, syncBpm — Phaser analógico", mapOf("speed" to 1.2f, "midcut" to 50f, "reso" to 50f, "fb" to 40f, "sync" to 0f, "syncBpm" to 80f)),
        CatalogItem(9, "VIBRATO", "Modulação", "speed (0.1..10.0), depth, sync, syncBpm — Vibrato de afinação", mapOf("speed" to 3.0f, "depth" to 40f, "sync" to 0f, "syncBpm" to 80f)),
        CatalogItem(10, "TRI VIBRATO", "Modulação", "speed (0.1..10.0), depth, sync, syncBpm — Triple Vibrato", mapOf("speed" to 3.0f, "depth" to 40f, "sync" to 0f, "syncBpm" to 80f)),
        CatalogItem(11, "OPTO VIBRATO", "Modulação", "speed (0.1..10.0), depth, sync, syncBpm — Vibrato óptico", mapOf("speed" to 3.0f, "depth" to 40f, "sync" to 0f, "syncBpm" to 80f)),
        CatalogItem(12, "UNIVIBE", "Psicodélico", "speed (0.1..10.0), depth, mix, sync, syncBpm — Univibe rotativo clássico", mapOf("speed" to 2.0f, "depth" to 55f, "mix" to 50f, "sync" to 0f, "syncBpm" to 80f)),
        CatalogItem(13, "TRI UNIVIBE", "Psicodélico", "speed (0.1..10.0), depth, mix, sync, syncBpm — Triple Univibe", mapOf("speed" to 2.0f, "depth" to 55f, "mix" to 50f, "sync" to 0f, "syncBpm" to 80f)),
        CatalogItem(14, "AUTOFILTER", "Filtro", "speed (0.1..10.0), min, max, mix, fb, sync, syncBpm — Auto-filtro dinâmico", mapOf("speed" to 2.0f, "min" to 40f, "max" to 60f, "mix" to 50f, "fb" to 50f, "sync" to 0f, "syncBpm" to 80f)),
        CatalogItem(15, "PHASER STEREO", "Estéreo", "speed (0.17..16.0), stage (A/B), regen, mix, lfo, sync, syncBpm — Phaser Estéreo", mapOf("speed" to 1.5f, "stage" to 1f, "regen" to 50f, "mix" to 50f, "lfo" to 1f, "sync" to 0f, "syncBpm" to 80f)),
        CatalogItem(16, "FLANGER STEREO", "Estéreo", "speed (0.17..16.0), depth, regen, mix, jet, lfo, sync, syncBpm — Flanger Estéreo", mapOf("speed" to 1.2f, "depth" to 45f, "regen" to 60f, "mix" to 35f, "jet" to 20f, "lfo" to 1f, "sync" to 0f, "syncBpm" to 80f)),
        CatalogItem(17, "VIBE STEREO", "Estéreo", "speed (0.17..16.0), stage (A/B), regen, mix, lfo, sync, syncBpm — Vibe Estéreo", mapOf("speed" to 2.0f, "stage" to 1f, "regen" to 50f, "mix" to 50f, "lfo" to 5f, "sync" to 0f, "syncBpm" to 80f)),
        CatalogItem(18, "CHORUS STEREO", "Estéreo", "speed (0.1..10.0), depth, mode (1..6), mix, sync, syncBpm — Chorus Estéreo", mapOf("speed" to 1.5f, "depth" to 45f, "mode" to 1f, "mix" to 35f, "sync" to 0f, "syncBpm" to 80f)),
        CatalogItem(19, "TREMOLO STEREO", "Estéreo", "speed (0.1..10.0), depth, level, mode (1..3), sync, syncBpm — Tremolo Estéreo", mapOf("speed" to 3.0f, "depth" to 50f, "level" to 70f, "mode" to 1f, "sync" to 0f, "syncBpm" to 80f)),
        CatalogItem(20, "VIBRATO STEREO", "Estéreo", "speed (0.1..10.0), depth (4..24), bits (0..100), mix (0..100), sync, syncBpm — Vibrato Estéreo", mapOf("speed" to 2.5f, "depth" to 12f, "bits" to 80f, "mix" to 50f, "sync" to 0f, "syncBpm" to 80f))
    )

    val DELAY_MODELS = listOf(
        CatalogItem(1, "Clean", "Mono", "Time (0.100..2.000s), Fb (0..100), Mix (0..100), Sync, SyncBPM (40..240) — Delay digital puro", mapOf("Time" to 500f, "Fb" to 30f, "Mix" to 25f, "Sync" to 0f, "SyncBPM" to 80f)),
        CatalogItem(2, "Modern", "Mono", "Time (0.100..2.000s), Fb, Mix, Phaser, Mod, Sync, SyncBPM — Delay moderno com Phaser", mapOf("Time" to 500f, "Fb" to 30f, "Mix" to 25f, "Phaser" to 0f, "Mod" to 0f, "Sync" to 0f, "SyncBPM" to 80f)),
        CatalogItem(3, "Echo", "Mono", "Time (0.100..2.000s), Fb, Mix, Sync, SyncBPM — Eco acústico natural", mapOf("Time" to 500f, "Fb" to 30f, "Mix" to 25f, "Sync" to 0f, "SyncBPM" to 80f)),
        CatalogItem(4, "Analog", "Mono", "Time (0.100..2.000s), Fb, Mix, Sync, SyncBPM — Delay analógico BBD quente", mapOf("Time" to 380f, "Fb" to 28f, "Mix" to 22f, "Sync" to 0f, "SyncBPM" to 80f)),
        CatalogItem(5, "Duck", "Mono", "Time, Fb, Mix, Release, Speed, Depth, Sync, SyncBPM — Ducking Delay dinâmico", mapOf("Time" to 500f, "Fb" to 30f, "Mix" to 28f, "Release" to 50f, "Speed" to 20f, "Depth" to 20f, "Sync" to 0f, "SyncBPM" to 80f)),
        CatalogItem(6, "Dtype", "Mono", "Time, Fb, Mix, Grit, Speed, Depth, Sync, SyncBPM — Tape Delay de fita", mapOf("Time" to 470f, "Fb" to 35f, "Mix" to 28f, "Grit" to 20f, "Speed" to 12f, "Depth" to 12f, "Sync" to 0f, "SyncBPM" to 80f)),
        CatalogItem(7, "Tremolo", "Mono", "Time, Fb, Mix, Pattern, Speed, Depth, Sync, SyncBPM — Tremolo Delay", mapOf("Time" to 500f, "Fb" to 30f, "Mix" to 28f, "Pattern" to 1f, "Speed" to 30f, "Depth" to 40f, "Sync" to 0f, "SyncBPM" to 80f)),
        CatalogItem(8, "Filter", "Mono", "Time, Fb, Mix, Filter, Speed, Depth, Sync, SyncBPM — Filter Delay", mapOf("Time" to 500f, "Fb" to 30f, "Mix" to 28f, "Filter" to 50f, "Speed" to 30f, "Depth" to 15f, "Sync" to 0f, "SyncBPM" to 80f)),
        CatalogItem(9, "Dual", "Mono", "Time, Fb, Mix, T-Mode, Speed, Depth, Sync, SyncBPM — Dual Delay", mapOf("Time" to 420f, "Fb" to 32f, "Mix" to 28f, "T-Mode" to 8f, "Speed" to 15f, "Depth" to 15f, "Sync" to 0f, "SyncBPM" to 80f)),
        CatalogItem(10, "Lofi", "Mono", "Time, Fb, Mix, Grit, Speed, Depth, Sync, SyncBPM — Lo-Fi Delay", mapOf("Time" to 500f, "Fb" to 30f, "Mix" to 28f, "Grit" to 25f, "Speed" to 15f, "Depth" to 15f, "Sync" to 0f, "SyncBPM" to 80f)),
        CatalogItem(11, "Pattern", "Mono", "Time, Fb, Mix, Pattern (0..15), Speed, Depth, Sync, SyncBPM — Pattern Delay", mapOf("Time" to 500f, "Fb" to 30f, "Mix" to 28f, "Pattern" to 8f, "Speed" to 15f, "Depth" to 15f, "Sync" to 0f, "SyncBPM" to 80f)),
        CatalogItem(12, "Ice", "Mono", "Time, Fb, Mix, Pitch (0..12), Mod, Sync, SyncBPM — Ice Pitch Delay", mapOf("Time" to 500f, "Fb" to 30f, "Mix" to 28f, "Pitch" to 6f, "Mod" to 30f, "Sync" to 0f, "SyncBPM" to 80f)),
        CatalogItem(13, "Reverse", "Mono", "Time, Fb, Mix, Phaser, Mod, Sync, SyncBPM — Reverse Delay", mapOf("Time" to 500f, "Fb" to 30f, "Mix" to 28f, "Phaser" to 10f, "Mod" to 15f, "Sync" to 0f, "SyncBPM" to 80f)),
        CatalogItem(14, "PingPong Stereo", "Estéreo", "Time (0.100..2.000s), Fb, Mix, Sync, SyncBPM — Ping-Pong Stereo Delay", mapOf("Time" to 500f, "Fb" to 30f, "Mix" to 28f, "Sync" to 0f, "SyncBPM" to 80f)),
        CatalogItem(15, "Clean Stereo", "Estéreo", "Time (0.100..2.000s), Fb, Mix, Sync, SyncBPM — Clean Stereo Delay", mapOf("Time" to 500f, "Fb" to 30f, "Mix" to 28f, "Sync" to 0f, "SyncBPM" to 80f)),
        CatalogItem(16, "Modern Stereo", "Estéreo", "Time, Fb, Mix, Phaser, Mod, Sync, SyncBPM — Modern Stereo Delay", mapOf("Time" to 500f, "Fb" to 30f, "Mix" to 28f, "Phaser" to 10f, "Mod" to 15f, "Sync" to 0f, "SyncBPM" to 80f)),
        CatalogItem(17, "Echo Stereo", "Estéreo", "Time, Fb, Mix, Sync, SyncBPM — Echo Stereo Delay", mapOf("Time" to 500f, "Fb" to 30f, "Mix" to 28f, "Sync" to 0f, "SyncBPM" to 80f)),
        CatalogItem(18, "Analog Stereo", "Estéreo", "Time, Fb, Mix, Sync, SyncBPM — Analog Stereo Delay", mapOf("Time" to 400f, "Fb" to 30f, "Mix" to 26f, "Sync" to 0f, "SyncBPM" to 80f)),
        CatalogItem(19, "Duck Stereo", "Estéreo", "Time, Fb, Mix, Release, Speed, Depth, Sync, SyncBPM — Duck Stereo Delay", mapOf("Time" to 500f, "Fb" to 30f, "Mix" to 28f, "Release" to 50f, "Speed" to 20f, "Depth" to 20f, "Sync" to 0f, "SyncBPM" to 80f)),
        CatalogItem(20, "Dtype Stereo", "Estéreo", "Time, Fb, Mix, Grit, Speed, Depth, Sync, SyncBPM — Dtype Stereo Tape Delay", mapOf("Time" to 470f, "Fb" to 35f, "Mix" to 28f, "Grit" to 20f, "Speed" to 12f, "Depth" to 12f, "Sync" to 0f, "SyncBPM" to 80f)),
        CatalogItem(21, "Tremolo Stereo", "Estéreo", "Time, Fb, Mix, Pattern, Dive, Speed, Depth, Sync, SyncBPM — Termolo Stereo Delay", mapOf("Time" to 500f, "Fb" to 30f, "Mix" to 28f, "Pattern" to 1f, "Dive" to 30f, "Speed" to 15f, "Depth" to 40f, "Sync" to 0f, "SyncBPM" to 80f)),
        CatalogItem(22, "Filter Stereo", "Estéreo", "Time, Fb, Mix, Filter, Speed, Depth, Sync, SyncBPM — Filter Stereo Delay", mapOf("Time" to 500f, "Fb" to 30f, "Mix" to 28f, "Filter" to 50f, "Speed" to 30f, "Depth" to 15f, "Sync" to 0f, "SyncBPM" to 80f)),
        CatalogItem(23, "Dual Stereo", "Estéreo", "Time, Fb, Mix, T-Mode, Speed, Depth, Sync, SyncBPM — Dual Stereo Delay", mapOf("Time" to 420f, "Fb" to 32f, "Mix" to 28f, "T-Mode" to 8f, "Speed" to 15f, "Depth" to 15f, "Sync" to 0f, "SyncBPM" to 80f)),
        CatalogItem(24, "Lofi Stereo", "Estéreo", "Time, Fb, Mix, Grit, Speed, Depth, Sync, SyncBPM — Lofi Stereo Delay", mapOf("Time" to 500f, "Fb" to 30f, "Mix" to 28f, "Grit" to 25f, "Speed" to 15f, "Depth" to 15f, "Sync" to 0f, "SyncBPM" to 80f)),
        CatalogItem(25, "Pattern Stereo", "Estéreo", "Time, Fb, Mix, Pattern (0..15), Speed, Depth, Sync, SyncBPM — Pattern Stereo Delay", mapOf("Time" to 500f, "Fb" to 30f, "Mix" to 28f, "Pattern" to 8f, "Speed" to 15f, "Depth" to 15f, "Sync" to 0f, "SyncBPM" to 80f)),
        CatalogItem(26, "Ice Stereo", "Estéreo", "Time, Fb, Mix, Pitch (0..12), Mod, Sync, SyncBPM — Ice Stereo Delay", mapOf("Time" to 500f, "Fb" to 30f, "Mix" to 28f, "Pitch" to 6f, "Mod" to 30f, "Sync" to 0f, "SyncBPM" to 80f)),
        CatalogItem(27, "Reverse Stereo", "Estéreo", "Time, Fb, Mix, Phaser, Mod, Sync, SyncBPM — Reverse Stereo Delay", mapOf("Time" to 500f, "Fb" to 30f, "Mix" to 28f, "Phaser" to 10f, "Mod" to 15f, "Sync" to 0f, "SyncBPM" to 80f))
    )

    val REVERB_MODELS = listOf(
        CatalogItem(1, "room", "Mono", "decay, mix, highPass, lowPass, modDepth — Simulação de sala acústica natural", mapOf("decay" to 20f, "mix" to 18f, "highPass" to 80f, "lowPass" to 80f, "modDepth" to 8f)),
        CatalogItem(2, "hall", "Mono", "decay, mix, highPass, lowPass, modDepth — Grande salão de concerto", mapOf("decay" to 28f, "mix" to 20f, "highPass" to 80f, "lowPass" to 80f, "modDepth" to 10f)),
        CatalogItem(3, "plate", "Mono", "decay, mix, highPass, lowPass, modDepth — Reverberação de placa metálica de estúdio", mapOf("decay" to 24f, "mix" to 18f, "highPass" to 80f, "lowPass" to 80f, "modDepth" to 10f)),
        CatalogItem(4, "spring", "Mono", "decay, mix, highPass, lowPass, combs — Tanque de molas vintage", mapOf("decay" to 25f, "mix" to 22f, "highPass" to 80f, "lowPass" to 80f, "combs" to 50f)),
        CatalogItem(5, "shimmer", "Mono", "decay, mix, tone, pitch, amount — Reverb cristalino com oitavas", mapOf("decay" to 50f, "mix" to 28f, "tone" to 70f, "pitch" to 40f, "amount" to 30f)),
        CatalogItem(6, "bloom", "Mono", "decay, mix, tone, lend, length — Reverb Bloom", mapOf("decay" to 35f, "mix" to 24f, "tone" to 75f, "lend" to 35f, "length" to 30f)),
        CatalogItem(7, "cloud", "Mono", "decay, mix, highPass, lowPass, diff — Cloud Reverb", mapOf("decay" to 45f, "mix" to 26f, "highPass" to 80f, "lowPass" to 80f, "diff" to 20f)),
        CatalogItem(8, "lofi", "Mono", "decay, mix, sampleRete, noiseLevel, modDepth — Lofi Reverb", mapOf("decay" to 25f, "mix" to 22f, "sampleRete" to 80f, "noiseLevel" to 25f, "modDepth" to 15f)),
        CatalogItem(9, "swell", "Mono", "decay, mix, tone, lend, riseTime — Swell Reverb", mapOf("decay" to 40f, "mix" to 26f, "tone" to 75f, "lend" to 35f, "riseTime" to 30f)),
        CatalogItem(10, "room stereo", "Estéreo", "decay, mix, highPass, lowPass, modDepth — Sala de estúdio em estéreo", mapOf("decay" to 20f, "mix" to 18f, "highPass" to 80f, "lowPass" to 80f, "modDepth" to 8f)),
        CatalogItem(11, "hall stereo", "Estéreo", "decay, mix, highPass, lowPass, modDepth — Hall em estéreo", mapOf("decay" to 32f, "mix" to 22f, "highPass" to 80f, "lowPass" to 80f, "modDepth" to 10f)),
        CatalogItem(12, "plate stereo", "Estéreo", "decay, mix, highPass, lowPass, modDepth — Plate em estéreo", mapOf("decay" to 26f, "mix" to 20f, "highPass" to 80f, "lowPass" to 80f, "modDepth" to 10f)),
        CatalogItem(13, "spring stereo", "Estéreo", "decay, mix, highPass, lowPass, combs — Spring em estéreo", mapOf("decay" to 26f, "mix" to 22f, "highPass" to 80f, "lowPass" to 80f, "combs" to 50f)),
        CatalogItem(14, "shimmer stereo", "Estéreo", "decay, mix, tone, pitch, amount — Shimmer em estéreo", mapOf("decay" to 60f, "mix" to 32f, "tone" to 72f, "pitch" to 40f, "amount" to 32f)),
        CatalogItem(15, "bloom stereo", "Estéreo", "decay, mix, tone, lend, length — Bloom em estéreo", mapOf("decay" to 38f, "mix" to 25f, "tone" to 75f, "lend" to 35f, "length" to 30f)),
        CatalogItem(16, "cloud stereo", "Estéreo", "decay, mix, highPass, lowPass, diff — Cloud em estéreo", mapOf("decay" to 50f, "mix" to 28f, "highPass" to 80f, "lowPass" to 80f, "diff" to 20f)),
        CatalogItem(17, "lofi stereo", "Estéreo", "decay, mix, sampleRete, noiseLevel, modDepth — Lofi em estéreo", mapOf("decay" to 25f, "mix" to 22f, "sampleRete" to 80f, "noiseLevel" to 25f, "modDepth" to 15f)),
        CatalogItem(18, "swell streo", "Estéreo", "decay, mix, tone, lend, riseTime — Swell em estéreo", mapOf("decay" to 42f, "mix" to 28f, "tone" to 75f, "lend" to 35f, "riseTime" to 30f))
    )

    fun normalizeKey(s: String): String = s.lowercase().replace("_", "").replace(" ", "").replace("-", "").replace(".", "")

    fun findAmp(nameOrId: String?): CatalogItem? {
        if (nameOrId.isNullOrBlank()) return null
        val trimmed = nameOrId.trim()
        val norm = normalizeKey(trimmed)
        AMP_MODELS.find {
            it.name.equals(trimmed, ignoreCase = true) ||
            normalizeKey(it.name) == norm
        }?.let { return it }
        val digits = trimmed.takeWhile { it.isDigit() }
        val id = digits.toIntOrNull()
        return if (id != null) AMP_MODELS.find { it.id == id } else null
    }

    fun findAmp(id: Int): CatalogItem? = AMP_MODELS.find { it.id == id }

    fun findCab(nameOrId: String?): CatalogItem? {
        if (nameOrId.isNullOrBlank()) return null
        val trimmed = nameOrId.trim()
        val norm = normalizeKey(trimmed)
        CAB_MODELS.find {
            it.name.equals(trimmed, ignoreCase = true) ||
            normalizeKey(it.name) == norm
        }?.let { return it }
        val digits = trimmed.takeWhile { it.isDigit() }
        val id = digits.toIntOrNull()
        return if (id != null) CAB_MODELS.find { it.id == id } else null
    }

    fun findCab(id: Int): CatalogItem? = CAB_MODELS.find { it.id == id }

    fun findDs(nameOrId: String?): CatalogItem? {
        if (nameOrId.isNullOrBlank()) return null
        val trimmed = nameOrId.trim()
        val norm = normalizeKey(trimmed)
        DS_MODELS.find {
            it.name.equals(trimmed, ignoreCase = true) ||
            normalizeKey(it.name) == norm
        }?.let { return it }
        when (norm) {
            "7mwavets1" -> return findDs("7M-VAVE_TS1")
            "8mwavets2" -> return findDs("8M-VAVE_TS2")
            "1bluesod", "bluesod", "bd2" -> return findDs("MW-SD1") ?: findDs("37BLUES_DR")
            "ts8", "ts808" -> return findDs("MW-808") ?: findDs("2TS8")
            "ds1" -> return findDs("MW-DS1") ?: findDs("3DS1")
            "ds2" -> return findDs("4DS2")
            "rat" -> return findDs("MW-RAT") ?: findDs("11RAT")
            "bigmuff", "bigdr" -> return findDs("MW-MUFF") ?: findDs("39BIG-MUFF")
            "slash", "slashuseyou" -> return findDs("SLASH_USE_YOU")
        }
        val digits = trimmed.takeWhile { it.isDigit() }
        val id = digits.toIntOrNull()
        return if (id != null) DS_MODELS.find { it.id == id } else null
    }

    fun findDs(id: Int): CatalogItem? = DS_MODELS.find { it.id == id }

    fun findFx(nameOrId: String?): CatalogItem? {
        if (nameOrId.isNullOrBlank()) return null
        val trimmed = nameOrId.trim()
        val norm = normalizeKey(trimmed)
        FX_MODELS.find {
            it.name.equals(trimmed, ignoreCase = true) ||
            normalizeKey(it.name) == norm
        }?.let { return it }
        if (norm == "compressor") return findFx("Compress")
        val digits = trimmed.takeWhile { it.isDigit() }
        val id = digits.toIntOrNull()
        return if (id != null) FX_MODELS.find { it.id == id } else null
    }

    fun findFx(id: Int): CatalogItem? = FX_MODELS.find { it.id == id }

    fun findNs(nameOrId: String?): CatalogItem? {
        if (nameOrId.isNullOrBlank()) return null
        val trimmed = nameOrId.trim()
        val norm = normalizeKey(trimmed)
        NS_MODELS.find {
            it.name.equals(trimmed, ignoreCase = true) ||
            normalizeKey(it.name) == norm
        }?.let { return it }
        when (norm) {
            "compress(ns)", "compressns" -> return findNs("compress")
            "compresspro(ns)", "compressprons" -> return findNs("compress pro")
            "fcompress(ns)", "fcompressns" -> return findNs("f compress")
            "noisegate", "gate" -> return findNs("soft gate")
        }
        val digits = trimmed.takeWhile { it.isDigit() }
        val id = digits.toIntOrNull()
        return if (id != null) NS_MODELS.find { it.id == id } else null
    }

    fun findNs(id: Int): CatalogItem? = NS_MODELS.find { it.id == id }

    fun findEq(nameOrId: String?): CatalogItem? {
        if (nameOrId.isNullOrBlank()) return null
        val trimmed = nameOrId.trim()
        val norm = normalizeKey(trimmed)
        EQ_MODELS.find {
            it.name.equals(trimmed, ignoreCase = true) ||
            normalizeKey(it.name) == norm
        }?.let { return it }
        when (norm) {
            "graphiceq", "5bandeq", "6bandeq", "guitareq" -> return findEq("Guitar EQ 6")
            "7bandeq", "basseq" -> return findEq("Bass EQ 7")
            "10bandeq", "normaleq", "parametriceq" -> return findEq("Normal EQ 10")
        }
        val digits = trimmed.takeWhile { it.isDigit() }
        val id = digits.toIntOrNull()
        return if (id != null) EQ_MODELS.find { it.id == id } else null
    }

    fun findEq(id: Int): CatalogItem? = EQ_MODELS.find { it.id == id }

    fun findMod(nameOrId: String?): CatalogItem? {
        if (nameOrId.isNullOrBlank()) return null
        val trimmed = nameOrId.trim()
        val norm = normalizeKey(trimmed)
        MOD_MODELS.find {
            it.name.equals(trimmed, ignoreCase = true) ||
            normalizeKey(it.name) == norm
        }?.let { return it }
        when (norm) {
            "dimensionalchorus" -> return findMod("CHORUS")
            "vintageflanger" -> return findMod("FLANGER")
            "phase90" -> return findMod("PHASER")
            "opticaltremolo" -> return findMod("OPTO TREMOLO")
        }
        val digits = trimmed.takeWhile { it.isDigit() }
        val id = digits.toIntOrNull()
        return if (id != null) MOD_MODELS.find { it.id == id } else null
    }

    fun findMod(id: Int): CatalogItem? = MOD_MODELS.find { it.id == id }

    fun findDelay(nameOrId: String?): CatalogItem? {
        if (nameOrId.isNullOrBlank()) return null
        val trimmed = nameOrId.trim()
        val norm = normalizeKey(trimmed)
        DELAY_MODELS.find {
            it.name.equals(trimmed, ignoreCase = true) ||
            normalizeKey(it.name) == norm
        }?.let { return it }
        when (norm) {
            "analogdelay" -> return findDelay("Analog")
            "digitaldelay", "digital", "dottedeighth", "slapback" -> return findDelay("Clean")
            "tapeecho", "tape" -> return findDelay("Dtype") ?: findDelay("Echo")
            "icedelay" -> return findDelay("Ice")
            "pingpong", "pingpongdelay" -> return findDelay("PingPong Stereo")
            "multitap" -> return findDelay("Pattern")
            "multitapstereo" -> return findDelay("Pattern Stereo")
            "dualmono" -> return findDelay("Dual")
            "mod", "modulation" -> return findDelay("Modern")
            "modulationstereo" -> return findDelay("Modern Stereo")
        }
        val digits = trimmed.takeWhile { it.isDigit() }
        val id = digits.toIntOrNull()
        return if (id != null) DELAY_MODELS.find { it.id == id } else null
    }

    fun findDelay(id: Int): CatalogItem? = DELAY_MODELS.find { it.id == id }

    fun findReverb(nameOrId: String?): CatalogItem? {
        if (nameOrId.isNullOrBlank()) return null
        val trimmed = nameOrId.trim()
        val norm = normalizeKey(trimmed)
        REVERB_MODELS.find {
            it.name.equals(trimmed, ignoreCase = true) ||
            normalizeKey(it.name) == norm
        }?.let { return it }
        when (norm) {
            "platereverb" -> return findReverb("plate")
            "shimmerreverb" -> return findReverb("shimmer stereo")
            "universe" -> return findReverb("shimmer")
            "coral" -> return findReverb("cloud")
            "swellstereo" -> return findReverb("swell streo")
            "hallreverb" -> return findReverb("hall")
            "spring63", "springreverb" -> return findReverb("spring")
            "roomreverb" -> return findReverb("room")
        }
        val digits = trimmed.takeWhile { it.isDigit() }
        val id = digits.toIntOrNull()
        return if (id != null) REVERB_MODELS.find { it.id == id } else null
    }

    fun findReverb(id: Int): CatalogItem? = REVERB_MODELS.find { it.id == id }

    fun getDefaultFxParams(modelName: String): Map<String, Float> {
        return findFx(modelName)?.defaultParams ?: mapOf("Sustain" to 50f, "Attack" to 50f, "Level" to 70f, "Blend" to 60f)
    }

    fun getDefaultNsParams(modelName: String): Map<String, Float> {
        return findNs(modelName)?.defaultParams ?: mapOf("thd" to -60f, "inputDetect" to 0f)
    }

    fun getDefaultDsParams(modelName: String): Map<String, Float> {
        return findDs(modelName)?.defaultParams ?: ds8Defaults()
    }

    fun getDefaultAmpParams(modelName: String): Map<String, Float> {
        return findAmp(modelName)?.defaultParams ?: ampDefaults("Overdrive")
    }

    fun getDefaultCabParams(modelName: String): Map<String, Float> {
        return findCab(modelName)?.defaultParams ?: mapOf("Level" to 80f, "Low Cut" to 80f, "High Cut" to 8000f)
    }

    fun getDefaultEqParams(modelName: String): Map<String, Float> {
        return findEq(modelName)?.defaultParams ?: EQ_MODELS.first().defaultParams
    }

    fun getDefaultModParams(modelName: String): Map<String, Float> {
        return findMod(modelName)?.defaultParams ?: MOD_MODELS.first().defaultParams // CHORUS
    }

    fun getDefaultDelayParams(modelName: String): Map<String, Float> {
        return findDelay(modelName)?.defaultParams ?: DELAY_MODELS[3].defaultParams // Analog
    }

    fun getDefaultReverbParams(modelName: String): Map<String, Float> {
        return findReverb(modelName)?.defaultParams ?: REVERB_MODELS[2].defaultParams // Plate
    }

    fun findItem(pedalKey: String, modelNameOrId: String?): CatalogItem? = when (pedalKey) {
        "FX" -> findFx(modelNameOrId)
        "NS", "NR" -> findNs(modelNameOrId)
        "DS" -> findDs(modelNameOrId)
        "AMP" -> findAmp(modelNameOrId)
        "CAB" -> findCab(modelNameOrId)
        "EQ" -> findEq(modelNameOrId)
        "MOD" -> findMod(modelNameOrId)
        "DELAY" -> findDelay(modelNameOrId)
        "REVERB" -> findReverb(modelNameOrId)
        else -> null
    }

    fun getItemsForPedal(pedalKey: String): List<CatalogItem> = when (pedalKey) {
        "FX" -> FX_MODELS
        "NS", "NR" -> NS_MODELS
        "DS" -> DS_MODELS
        "AMP" -> AMP_MODELS
        "CAB" -> CAB_MODELS
        "EQ" -> EQ_MODELS
        "MOD" -> MOD_MODELS
        "DELAY" -> DELAY_MODELS
        "REVERB" -> REVERB_MODELS
        else -> emptyList()
    }
}
