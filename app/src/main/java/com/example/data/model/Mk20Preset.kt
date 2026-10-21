package com.example.data.model

import org.json.JSONObject

data class Mk20Preset(
    val title: String,
    val style: String = "Geral",
    val bpm: Int = 120,
    val youtubeLink: String = "",
    val details: String = "",
    val pedals: Mk20Pedals = Mk20Pedals()
) {
    fun toJsonString(indent: Int = 2): String {
        val root = JSONObject()
        root.put("title", title)
        root.put("style", style)
        root.put("bpm", bpm)
        if (youtubeLink.isNotBlank()) {
            root.put("youtube_link", youtubeLink)
        }
        if (details.isNotBlank()) {
            root.put("details", details)
        }

        val pedalsObj = JSONObject()
        pedals.NR?.let { pedalsObj.put("NR", it.toJsonObject()) }
        pedals.DS?.let { pedalsObj.put("DS", it.toJsonObject()) }
        pedals.AMP?.let { pedalsObj.put("AMP", it.toJsonObject()) }
        pedals.CAB?.let { pedalsObj.put("CAB", it.toJsonObject()) }
        pedals.MOD?.let { pedalsObj.put("MOD", it.toJsonObject()) }
        pedals.DELAY?.let { pedalsObj.put("DELAY", it.toJsonObject()) }
        pedals.REVERB?.let { pedalsObj.put("REVERB", it.toJsonObject()) }

        root.put("pedals", pedalsObj)
        return if (indent > 0) root.toString(indent) else root.toString()
    }

    companion object {
        fun fromJsonString(jsonStr: String): Mk20Preset {
            val root = JSONObject(jsonStr)
            val title = root.optString("title", "Preset MK-20")
            val style = root.optString("style", "Personalizado")
            val bpm = root.optInt("bpm", 120)
            val yt = root.optString("youtube_link", "")
            val details = root.optString("details", "")

            val pedalsObj = root.optJSONObject("pedals") ?: JSONObject()
            val pedals = Mk20Pedals(
                NR = pedalsObj.optJSONObject("NR")?.let { PedalModule.fromJsonObject(it) },
                DS = pedalsObj.optJSONObject("DS")?.let { PedalModule.fromJsonObject(it) },
                AMP = pedalsObj.optJSONObject("AMP")?.let { PedalModule.fromJsonObject(it) },
                CAB = pedalsObj.optJSONObject("CAB")?.let { PedalModule.fromJsonObject(it) },
                MOD = pedalsObj.optJSONObject("MOD")?.let { PedalModule.fromJsonObject(it) },
                DELAY = pedalsObj.optJSONObject("DELAY")?.let { PedalModule.fromJsonObject(it) },
                REVERB = pedalsObj.optJSONObject("REVERB")?.let { PedalModule.fromJsonObject(it) }
            )

            return Mk20Preset(
                title = title,
                style = style,
                bpm = bpm,
                youtubeLink = yt,
                details = details,
                pedals = pedals
            )
        }

        val DEFAULT_PRESET = Mk20Preset(
            title = "Worship Ambient Clean",
            style = "Worship / Ambient",
            bpm = 110,
            youtubeLink = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            details = "Guitarra Stratocaster na posição 4, som limpo cristalino com delay colcheia pontuada e shimmer sutil.",
            pedals = Mk20Pedals(
                NR = PedalModule(name = "Noise Gate", active = true, model = "Noise Gate", params = mapOf("Thresh" to 30f, "Decay" to 50f)),
                DS = PedalModule(name = "Drive / Boost", active = true, model = "1BLUES OD", params = mapOf("Gain" to 28f, "Tone" to 55f, "Level" to 60f)),
                AMP = PedalModule(name = "Amplificador", active = true, model = "26CL2_JC120", params = mapOf("Gain" to 35f, "Bass" to 48f, "Mid" to 62f, "Treble" to 65f, "Volume" to 70f)),
                CAB = PedalModule(name = "Caixa / IR", active = true, model = "61RJ120azz212", params = mapOf("Volume" to 80f)),
                MOD = PedalModule(name = "Modulação", active = true, model = "Dimensional Chorus", params = mapOf("Speed" to 30f, "Depth" to 45f, "Mix" to 35f)),
                DELAY = PedalModule(name = "Delay", active = true, model = "Analog Delay", params = mapOf("Time" to 340f, "Repeats" to 38f, "Mix" to 32f)),
                REVERB = PedalModule(name = "Reverb", active = true, model = "Plate Reverb", params = mapOf("Decay" to 60f, "Tone" to 55f, "Mix" to 40f))
            )
        )

        val FACTORY_PRESETS = listOf(
            DEFAULT_PRESET,
            Mk20Preset(
                title = "Sweet Child O' Mine Lead",
                style = "Hard Rock Solo",
                bpm = 125,
                youtubeLink = "https://www.youtube.com/watch?v=1w7OgIMMRc4",
                details = "Captador do braço Humbucker, afinação em Eb, drive encorpado clássico do Guns N' Roses.",
                pedals = Mk20Pedals(
                    NR = PedalModule(name = "Noise Gate", active = true, model = "Noise Gate", params = mapOf("Thresh" to 45f, "Decay" to 40f)),
                    DS = PedalModule(name = "Drive / Boost", active = true, model = "2TS8", params = mapOf("Gain" to 40f, "Tone" to 60f, "Level" to 65f)),
                    AMP = PedalModule(name = "Amplificador", active = true, model = "860D_JCM800", params = mapOf("Gain" to 75f, "Bass" to 55f, "Mid" to 70f, "Treble" to 65f, "Volume" to 72f)),
                    CAB = PedalModule(name = "Caixa / IR", active = true, model = "36Mar60B412A", params = mapOf("Volume" to 82f)),
                    MOD = PedalModule(name = "Modulação", active = false, model = "Dimensional Chorus", params = mapOf("Speed" to 20f, "Depth" to 30f, "Mix" to 20f)),
                    DELAY = PedalModule(name = "Delay", active = true, model = "Tape Echo", params = mapOf("Time" to 420f, "Repeats" to 30f, "Mix" to 28f)),
                    REVERB = PedalModule(name = "Reverb", active = true, model = "Room Reverb", params = mapOf("Decay" to 35f, "Tone" to 50f, "Mix" to 25f))
                )
            ),
            Mk20Preset(
                title = "Comfortably Numb Solo",
                style = "Classic Rock / Pink Floyd",
                bpm = 127,
                youtubeLink = "https://www.youtube.com/watch?v=_FrOQC-zEog",
                details = "Sustain infinito de David Gilmour com Big Muff, Hiwatt clean e delays analógicos estéreo.",
                pedals = Mk20Pedals(
                    NR = PedalModule(name = "Noise Gate", active = true, model = "Noise Gate", params = mapOf("Thresh" to 35f, "Decay" to 50f)),
                    DS = PedalModule(name = "Drive / Boost", active = true, model = "18Big_Muff", params = mapOf("Gain" to 70f, "Tone" to 48f, "Level" to 65f)),
                    AMP = PedalModule(name = "Amplificador", active = true, model = "13CL1_FenDi", params = mapOf("Gain" to 40f, "Bass" to 50f, "Mid" to 60f, "Treble" to 60f, "Volume" to 75f)),
                    CAB = PedalModule(name = "Caixa / IR", active = true, model = "11FdTR65R212B", params = mapOf("Volume" to 80f)),
                    MOD = PedalModule(name = "Modulação", active = true, model = "Vintage Flanger", params = mapOf("Speed" to 25f, "Depth" to 60f, "Mix" to 40f)),
                    DELAY = PedalModule(name = "Delay", active = true, model = "Digital Delay", params = mapOf("Time" to 480f, "Repeats" to 45f, "Mix" to 35f)),
                    REVERB = PedalModule(name = "Reverb", active = true, model = "Hall Reverb", params = mapOf("Decay" to 65f, "Tone" to 50f, "Mix" to 35f))
                )
            ),
            Mk20Preset(
                title = "Texas Blues Crunch",
                style = "Blues Rock / SRV",
                bpm = 115,
                youtubeLink = "https://www.youtube.com/watch?v=31qqJk6Oa30",
                details = "Ataque forte de palheta com Tube Screamer empurrando Fender valvulado no limite do breakup.",
                pedals = Mk20Pedals(
                    NR = PedalModule(name = "Noise Gate", active = false, model = "Noise Gate", params = mapOf("Thresh" to 20f, "Decay" to 40f)),
                    DS = PedalModule(name = "Drive / Boost", active = true, model = "2TS8", params = mapOf("Gain" to 35f, "Tone" to 65f, "Level" to 75f)),
                    AMP = PedalModule(name = "Amplificador", active = true, model = "13CL1_FenDi", params = mapOf("Gain" to 55f, "Bass" to 60f, "Mid" to 65f, "Treble" to 70f, "Volume" to 75f)),
                    CAB = PedalModule(name = "Caixa / IR", active = true, model = "11FdTR65R212B", params = mapOf("Volume" to 82f)),
                    MOD = PedalModule(name = "Modulação", active = false, model = "Optical Tremolo", params = mapOf("Speed" to 50f, "Depth" to 50f, "Mix" to 30f)),
                    DELAY = PedalModule(name = "Delay", active = false, model = "Analog Delay", params = mapOf("Time" to 220f, "Repeats" to 20f, "Mix" to 20f)),
                    REVERB = PedalModule(name = "Reverb", active = true, model = "Spring 63", params = mapOf("Decay" to 45f, "Tone" to 60f, "Mix" to 35f))
                )
            ),
            Mk20Preset(
                title = "Heavy Modern Metal Chug",
                style = "Metalcore / Djent",
                bpm = 140,
                youtubeLink = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                details = "Graves apertados, Noise Gate rápido para cortes staccato e ganho ENGL saturado.",
                pedals = Mk20Pedals(
                    NR = PedalModule(name = "Noise Gate", active = true, model = "Noise Gate", params = mapOf("Thresh" to 65f, "Decay" to 25f)),
                    DS = PedalModule(name = "Drive / Boost", active = true, model = "2TS8", params = mapOf("Gain" to 15f, "Tone" to 75f, "Level" to 85f)),
                    AMP = PedalModule(name = "Amplificador", active = true, model = "75DS ENGL 120", params = mapOf("Gain" to 78f, "Bass" to 45f, "Mid" to 70f, "Treble" to 68f, "Volume" to 70f)),
                    CAB = PedalModule(name = "Caixa / IR", active = true, model = "45Mes412Trad", params = mapOf("Volume" to 85f)),
                    MOD = PedalModule(name = "Modulação", active = false, model = "Dimensional Chorus", params = mapOf("Speed" to 30f, "Depth" to 40f, "Mix" to 25f)),
                    DELAY = PedalModule(name = "Delay", active = false, model = "Digital Delay", params = mapOf("Time" to 350f, "Repeats" to 25f, "Mix" to 20f)),
                    REVERB = PedalModule(name = "Reverb", active = true, model = "Room Reverb", params = mapOf("Decay" to 25f, "Tone" to 50f, "Mix" to 15f))
                )
            )
        )
    }
}

data class Mk20Pedals(
    val NR: PedalModule? = PedalModule("Noise Gate", true, "Noise Gate", mapOf("Thresh" to 30f, "Decay" to 50f)),
    val DS: PedalModule? = PedalModule("Drive / Boost", true, "1BLUES OD", mapOf("Gain" to 30f, "Tone" to 50f, "Level" to 60f)),
    val AMP: PedalModule? = PedalModule("Amplificador", true, "26CL2_JC120", mapOf("Gain" to 35f, "Bass" to 50f, "Mid" to 60f, "Treble" to 65f, "Volume" to 70f)),
    val CAB: PedalModule? = PedalModule("Caixa / IR", true, "61RJ120azz212", mapOf("Volume" to 80f)),
    val MOD: PedalModule? = PedalModule("Modulação", true, "Dimensional Chorus", mapOf("Speed" to 30f, "Depth" to 50f, "Mix" to 35f)),
    val DELAY: PedalModule? = PedalModule("Delay", true, "Analog Delay", mapOf("Time" to 340f, "Repeats" to 35f, "Mix" to 30f)),
    val REVERB: PedalModule? = PedalModule("Reverb", true, "Plate Reverb", mapOf("Decay" to 50f, "Tone" to 55f, "Mix" to 35f))
)

data class PedalModule(
    val name: String,
    val active: Boolean = true,
    val model: String,
    val params: Map<String, Float> = emptyMap()
) {
    fun toJsonObject(): JSONObject {
        val obj = JSONObject()
        obj.put("name", name)
        obj.put("active", active)
        obj.put("model", model)
        val paramsObj = JSONObject()
        params.forEach { (k, v) -> paramsObj.put(k, v.toInt()) }
        obj.put("params", paramsObj)
        return obj
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): PedalModule {
            val name = obj.optString("name", "Efeito")
            val active = obj.optBoolean("active", true)
            val model = obj.optString("model", "Padrão")
            val paramsObj = obj.optJSONObject("params")
            val paramsMap = mutableMapOf<String, Float>()
            paramsObj?.keys()?.forEach { k ->
                paramsMap[k] = paramsObj.optDouble(k, 50.0).toFloat()
            }
            return PedalModule(
                name = name,
                active = active,
                model = model,
                params = paramsMap
            )
        }
    }
}
