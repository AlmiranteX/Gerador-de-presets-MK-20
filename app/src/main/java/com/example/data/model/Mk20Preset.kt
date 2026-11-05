package com.example.data.model

import org.json.JSONObject

data class Mk20Preset(
    val title: String,
    val style: String = "Geral",
    val bpm: Int = 120,
    val masterVolume: Int = 40,
    val youtubeLink: String = "",
    val details: String = "",
    val pedals: Mk20Pedals = Mk20Pedals()
) {
    fun toJsonString(indent: Int = 2): String {
        val root = JSONObject()
        root.put("title", title)
        root.put("style", style)
        root.put("bpm", bpm)
        root.put("master_volume", masterVolume)
        if (youtubeLink.isNotBlank()) {
            root.put("youtube_link", youtubeLink)
        }
        if (details.isNotBlank()) {
            root.put("details", details)
        }

        val pedalsObj = JSONObject()
        pedals.FX?.let { pedalsObj.put("FX", it.toJsonObject()) }
        pedals.NS?.let { pedalsObj.put("NS", it.toJsonObject()) }
        pedals.DS?.let { pedalsObj.put("DS", it.toJsonObject()) }
        pedals.AMP?.let { pedalsObj.put("AMP", it.toJsonObject()) }
        pedals.CAB?.let { pedalsObj.put("CAB", it.toJsonObject()) }
        pedals.EQ?.let { pedalsObj.put("EQ", it.toJsonObject()) }
        pedals.MOD?.let { pedalsObj.put("MOD", it.toJsonObject()) }
        pedals.DELAY?.let { pedalsObj.put("DELAY", it.toJsonObject()) }
        pedals.REVERB?.let { pedalsObj.put("REVERB", it.toJsonObject()) }
        pedals.VOL?.let { pedalsObj.put("VOL", it.toJsonObject()) }

        root.put("pedals", pedalsObj)
        return if (indent > 0) root.toString(indent) else root.toString()
    }

    /**
     * Serializa este preset no formato binário oficial da M-VAVE MK-20 (.mk20 ou .amk20).
     * @param targetSize Tamanho exato em bytes (ex: 512 padrão M-EFCS, 256, 1024)
     * @param templateBytes Molde opcional extraído diretamente do pedal do usuário
     */
    fun toBinaryMk20(
        targetSize: Int = com.example.util.Mk20BinarySerializer.AUTHENTIC_MK20_SIZE,
        useAmk2Header: Boolean = false,
        templateBytes: ByteArray? = null
    ): ByteArray {
        return com.example.util.Mk20BinarySerializer.serialize(
            preset = this,
            targetSize = targetSize,
            useAmk2Header = useAmk2Header,
            templateBytes = templateBytes
        )
    }

    companion object {
        fun fromBinary(bytes: ByteArray): Mk20Preset {
            return com.example.util.Mk20BinarySerializer.deserialize(bytes)
        }

        fun fromJsonString(jsonStr: String): Mk20Preset {
            val root = JSONObject(jsonStr)
            val title = root.optString("title", "Preset MK-20")
            val style = root.optString("style", "Personalizado")
            val bpm = root.optInt("bpm", 120)
            val masterVol = root.optInt("master_volume", 40)
            val yt = root.optString("youtube_link", "")
            val details = root.optString("details", "")

            val pedalsObj = root.optJSONObject("pedals") ?: JSONObject()
            // Backwards compatibility for NR -> NS
            val nsModule = pedalsObj.optJSONObject("NS")?.let { PedalModule.fromJsonObject(it) }
                ?: pedalsObj.optJSONObject("NR")?.let { PedalModule.fromJsonObject(it) }

            val pedals = Mk20Pedals(
                FX = pedalsObj.optJSONObject("FX")?.let { PedalModule.fromJsonObject(it) } ?: PedalModule("Pré-Efeito / Dinâmica", true, "Compress", mapOf("Sustain" to 4.5f, "Attack" to 50f, "Level" to 70f, "Blend" to 60f)),
                NS = nsModule ?: PedalModule("Noise Suppressor", true, "soft gate", mapOf("thd" to -60f, "inputDetect" to 0f)),
                DS = pedalsObj.optJSONObject("DS")?.let { PedalModule.fromJsonObject(it) },
                AMP = pedalsObj.optJSONObject("AMP")?.let { PedalModule.fromJsonObject(it) },
                CAB = pedalsObj.optJSONObject("CAB")?.let { PedalModule.fromJsonObject(it) },
                EQ = pedalsObj.optJSONObject("EQ")?.let { PedalModule.fromJsonObject(it) } ?: PedalModule("Equalizador", false, "Guitar EQ 6", mapOf("100Hz" to 0f, "200Hz" to 0f, "400Hz" to 0f, "800Hz" to 0f, "1.6kHz" to 0f, "3.2kHz" to 0f)),
                MOD = pedalsObj.optJSONObject("MOD")?.let { PedalModule.fromJsonObject(it) },
                DELAY = pedalsObj.optJSONObject("DELAY")?.let { PedalModule.fromJsonObject(it) },
                REVERB = pedalsObj.optJSONObject("REVERB")?.let { PedalModule.fromJsonObject(it) },
                VOL = pedalsObj.optJSONObject("VOL")?.let { PedalModule.fromJsonObject(it) } ?: PedalModule("Master Volume", true, "Master", mapOf("Level" to 85f))
            )

            return Mk20Preset(
                title = title,
                style = style,
                bpm = bpm,
                masterVolume = masterVol,
                youtubeLink = yt,
                details = details,
                pedals = pedals
            )
        }

        val DEFAULT_PRESET = Mk20Preset(
            title = "Worship Ambient Clean",
            style = "Worship / Ambient",
            bpm = 110,
            masterVolume = 60,
            youtubeLink = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            details = "Guitarra Stratocaster na posição 4, som limpo cristalino com compressor suave, delay colcheia pontuada e shimmer sutil.",
            pedals = Mk20Pedals(
                FX = PedalModule(name = "Pré-Efeito / Dinâmica", active = true, model = "Compress", params = mapOf("Sustain" to 4.5f, "Attack" to 50f, "Level" to 70f, "Blend" to 60f)),
                NS = PedalModule(name = "Noise Suppressor", active = true, model = "soft gate", params = mapOf("thd" to -60f, "inputDetect" to 0f)),
                DS = PedalModule(name = "Drive / Boost", active = true, model = "MW-808", params = mapOf("gain" to 0f, "tone" to 50f, "level" to 100f)),
                AMP = PedalModule(name = "Amplificador", active = true, model = "26CL2_JC120", params = mapOf("Gain" to 28f, "Level" to 76f, "Bass" to 48f, "Middle" to 60f, "Treble" to 66f, "Reso" to 45f, "Pres" to 52f, "Bright" to 75f)),
                CAB = PedalModule(name = "Caixa / IR", active = true, model = "20FdTwRev212A", params = mapOf("Level" to 82f, "Low Cut" to 75f, "High Cut" to 7500f)),
                EQ = PedalModule(name = "Equalizador", active = true, model = "Guitar EQ 6", params = mapOf("100Hz" to 0f, "200Hz" to 0f, "400Hz" to -2f, "800Hz" to 1f, "1.6kHz" to 2f, "3.2kHz" to 2f)),
                MOD = PedalModule(name = "Modulação", active = true, model = "CHORUS STEREO", params = mapOf("speed" to 1.0f, "depth" to 35f, "mode" to 1f, "mix" to 30f, "sync" to 0f, "syncBpm" to 80f)),
                DELAY = PedalModule(name = "Delay", active = true, model = "Analog", params = mapOf("Time" to 340f, "Fb" to 32f, "Mix" to 24f, "Sync" to 0f, "SyncBPM" to 80f)),
                REVERB = PedalModule(name = "Reverb", active = true, model = "shimmer stereo", params = mapOf("decay" to 58f, "mix" to 26f, "tone" to 65f, "pitch" to 40f, "amount" to 28f)),
                VOL = PedalModule(name = "Master Volume", active = true, model = "Master", params = mapOf("Level" to 85f))
            )
        )

        val FACTORY_PRESETS: List<Mk20Preset> get() = listOf(
            DEFAULT_PRESET
        ) + Mk20MasterSongDatabase.SONGS.map { it.preset }
    }
}

data class Mk20Pedals(
    val FX: PedalModule? = PedalModule("Pré-Efeito / Dinâmica", true, "Compress", mapOf("Sustain" to 4.5f, "Attack" to 48f, "Level" to 72f, "Blend" to 60f)),
    val NS: PedalModule? = PedalModule("Noise Suppressor", true, "soft gate", mapOf("thd" to -58f, "inputDetect" to 0f)),
    val DS: PedalModule? = PedalModule(
        name = "Drive / Boost",
        active = true,
        model = "MW-808",
        params = mapOf(
            "gain" to 10f,
            "tone" to 52f,
            "level" to 95f
        )
    ),
    val AMP: PedalModule? = PedalModule("Amplificador", true, "51OD1_MS2203", mapOf("Gain" to 58f, "Level" to 76f, "Bass" to 54f, "Middle" to 66f, "Treble" to 62f, "Reso" to 52f, "Pres" to 56f, "Bright" to 50f)),
    val CAB: PedalModule? = PedalModule("Caixa / IR", true, "33Mar60A412A", mapOf("Level" to 82f, "Low Cut" to 80f, "High Cut" to 6800f)),
    val EQ: PedalModule? = PedalModule("Equalizador", true, "Guitar EQ 6", mapOf("100Hz" to 1f, "200Hz" to -1f, "400Hz" to -2f, "800Hz" to 1f, "1.6kHz" to 2f, "3.2kHz" to 2f)),
    val MOD: PedalModule? = PedalModule("Modulação", false, "CHORUS", mapOf("speed" to 1.0f, "depth" to 40f, "mix" to 35f, "sync" to 0f, "syncBpm" to 120f)),
    val DELAY: PedalModule? = PedalModule("Delay", true, "Analog", mapOf("Time" to 350f, "Fb" to 25f, "Mix" to 22f, "Sync" to 0f, "SyncBPM" to 120f)),
    val REVERB: PedalModule? = PedalModule("Reverb", true, "plate", mapOf("decay" to 22f, "mix" to 18f, "highPass" to 80f, "lowPass" to 80f, "modDepth" to 10f)),
    val VOL: PedalModule? = PedalModule("Master Volume", true, "Master", mapOf("Level" to 85f)),
    val NR: PedalModule? = null
) {
    val activeNS: PedalModule? get() = NS ?: NR
}

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
        params.forEach { (k, v) ->
            if (kotlin.math.abs(v - v.toInt().toFloat()) > 0.01f) {
                paramsObj.put(k, v.toDouble())
            } else {
                paramsObj.put(k, v.toInt())
            }
        }
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
                paramsMap[k] = paramsObj.optDouble(k, 0.0).toFloat()
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

