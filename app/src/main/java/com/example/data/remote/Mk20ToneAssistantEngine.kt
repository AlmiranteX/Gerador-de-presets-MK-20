package com.example.data.remote

import com.example.data.model.Mk20Catalog
import com.example.data.model.Mk20Pedals
import com.example.data.model.Mk20Preset
import com.example.data.model.PedalModule
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ToneAssistantResult(
    val explanation: String,
    val appliedChanges: List<String>,
    val updatedPreset: Mk20Preset
)

object Mk20ToneAssistantEngine {

    private const val SYSTEM_PROMPT = """
Você é o Roadie Oficial e Engenheiro de Timbre especialista da pedaleira M-VAVE MK-20.
Sua missão é conversar com o guitarrista, entender o que ele quer no som da guitarra e ajustar ou criar presets oficiais da MK-20.

Você DEVE responder SEMPRE em formato JSON estrito contendo:
{
  "explanation": "Explicação musical concisa em português (1 a 3 frases) do que foi ajustado tecnicamente e como isso atende ao pedido do músico.",
  "changes": [
    "Redução de agudos em 3.2kHz (-2dB)",
    "Amaciamento de presença no amplificador",
    "High Cut do gabinete ajustado para 6.5kHz"
  ],
  "preset": {
    "title": "Nome do preset",
    "style": "Estilo",
    "bpm": 120,
    "master_volume": 40,
    "pedals": {
      "FX": { "name": "Pré-Efeito / Dinâmica", "active": true, "model": "Compress", "params": { "Sustain": 4.5, "Attack": 48, "Level": 72, "Blend": 60 } },
      "NS": { "name": "Noise Suppressor", "active": true, "model": "soft gate", "params": { "thd": -58, "inputDetect": 0 } },
      "DS": { "name": "Drive / Boost", "active": true, "model": "MW-808", "params": { "gain": 12, "tone": 52, "level": 92 } },
      "AMP": { "name": "Amplificador", "active": true, "model": "26CL2_JC120", "params": { "Gain": 32, "Level": 78, "Bass": 50, "Middle": 60, "Treble": 66, "Reso": 48, "Pres": 56, "Bright": 68 } },
      "CAB": { "name": "Caixa / IR", "active": true, "model": "20FdTwRev212A", "params": { "Level": 84, "Low Cut": 78, "High Cut": 7800 } },
      "EQ": { "name": "Equalizador", "active": true, "model": "Guitar EQ 6", "params": { "100Hz": 0, "200Hz": 0, "400Hz": -2, "800Hz": 1, "1.6kHz": 2, "3.2kHz": 2 } },
      "MOD": { "name": "Modulação", "active": false, "model": "CHORUS STEREO", "params": { "speed": 1.2, "depth": 38, "mix": 30, "sync": 0, "syncBpm": 120 } },
      "DELAY": { "name": "Delay", "active": true, "model": "Analog", "params": { "Time": 350, "Fb": 25, "Mix": 20, "Sync": 0, "SyncBPM": 120 } },
      "REVERB": { "name": "Reverb", "active": true, "model": "plate", "params": { "decay": 22, "mix": 18 } }
    }
  }
}

REGRAS OBRIGATÓRIAS DE ENGENHARIA DE ÁUDIO OFICIAL MK-20:
- VOLUME E BRILHO OFICIAIS: Mantenha master_volume entre 62 e 68, AMP Level em 78, CAB Level em 84-85, Bright Switch ativo (65+) e High Cut aberto entre 7500Hz e 8000Hz (exceto se o usuário pedir explicitamente para amaciar agudos).
- NOISE GATE TRANSPARENTE: Para som limpo, use "thd": -58. Para crunch/drive/solo, use "thd": -52 para nunca engolir o sustain das notas.
- Retorne APENAS o JSON puro, sem textos antes ou depois.
"""

    suspend fun consultAndAdjust(
        currentPreset: Mk20Preset,
        userMessage: String,
        customApiKey: String?
    ): ToneAssistantResult = withContext(Dispatchers.IO) {
        val apiKey = when {
            !customApiKey.isNullOrBlank() -> customApiKey
            BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" -> BuildConfig.GEMINI_API_KEY
            else -> null
        }

        if (apiKey != null) {
            try {
                val prompt = """
Preset Atual na Pedaleira:
${currentPreset.toJsonString(indent = 2)}

Pedido do Músico:
"$userMessage"

Pesquise se necessário o timbre de referência solicitado e ajuste os parâmetros manteniendo o volume cheio e o brilho oficial da MK-20. Retorne estritamente o JSON com explanation, changes e preset.
""".trimIndent()

                val response = GeminiClient.generateWithSearchGrounding(
                    apiKey = apiKey,
                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                    systemInstruction = Content(parts = listOf(Part(text = SYSTEM_PROMPT)))
                )
                val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!rawText.isNullOrBlank()) {
                    val cleanJson = cleanJsonString(rawText)
                    val root = JSONObject(cleanJson)
                    val explanation = root.optString("explanation", "Ajustei seu timbre conforme solicitado!")
                    val changesList = mutableListOf<String>()
                    val changesArr = root.optJSONArray("changes")
                    if (changesArr != null) {
                        for (i in 0 until changesArr.length()) {
                            changesList.add(changesArr.optString(i))
                        }
                    }
                    val presetJson = root.optJSONObject("preset")
                    val updatedPreset = if (presetJson != null) {
                        Mk20Preset.fromJsonString(presetJson.toString())
                    } else {
                        adjustLocally(currentPreset, userMessage).updatedPreset
                    }
                    val sanitized = com.example.util.AcousticStudioGuardrail.sanitize(updatedPreset)

                    return@withContext ToneAssistantResult(
                        explanation = explanation,
                        appliedChanges = if (changesList.isNotEmpty()) changesList else listOf("Parâmetros otimizados para o seu pedido"),
                        updatedPreset = sanitized
                    )
                }
            } catch (e: Exception) {
                // Em caso de falha de conexão ou API, utiliza o motor inteligente local
            }
        }

        // Motor local de adaptação musical por regras especializadas da MK-20
        adjustLocally(currentPreset, userMessage)
    }

    private fun cleanJsonString(raw: String): String {
        return raw.replace("```json", "")
            .replace("```", "")
            .trim()
    }

    fun adjustLocally(currentPreset: Mk20Preset, userMessage: String): ToneAssistantResult {
        val query = userMessage.lowercase().trim()
        val pedals = currentPreset.pedals
        val ampParams = pedals.AMP?.params?.toMutableMap() ?: mutableMapOf()
        val cabParams = pedals.CAB?.params?.toMutableMap() ?: mutableMapOf()
        val eqParams = pedals.EQ?.params?.toMutableMap() ?: mutableMapOf()
        val dsParams = pedals.DS?.params?.toMutableMap() ?: mutableMapOf()
        val revParams = pedals.REVERB?.params?.toMutableMap() ?: mutableMapOf()
        val dlyParams = pedals.DELAY?.params?.toMutableMap() ?: mutableMapOf()

        val changes = mutableListOf<String>()
        var explanation = ""
        var newTitle = currentPreset.title

        when {
            // 1. Agudos excessivos / Strato / Tele / Som estridente ou ardido
            query.contains("agudo") || query.contains("strato") || query.contains("tele") ||
            query.contains("ardido") || query.contains("estridente") || query.contains("amaciar") ||
            query.contains("suave") || query.contains("aveludado") -> {
                ampParams["Treble"] = (ampParams["Treble"] ?: 60f) - 12f
                ampParams["Pres"] = (ampParams["Pres"] ?: 55f) - 10f
                if ((ampParams["Bright"] ?: 0f) > 60f) {
                    ampParams["Bright"] = 50f
                }
                cabParams["High Cut"] = 6500f // 6.5 kHz
                eqParams["3.2kHz"] = -2f
                eqParams["1.6kHz"] = -1f
                eqParams["800Hz"] = (eqParams["800Hz"] ?: 0f) + 1f
                if (dsParams.containsKey("tone")) {
                    dsParams["tone"] = 45f
                }
                newTitle = if (!newTitle.contains("Suave")) "$newTitle (Suave)" else newTitle
                changes.add("Treble (-12) e Presença (-10) reduzidos no amplificador")
                changes.add("High Cut da Caixa ajustado para 6.5kHz (elimina ardência)")
                changes.add("Atenuação de -2dB em 3.2kHz no Equalizador")
                changes.add("Médios encorpados (+1dB em 800Hz) para dar corpo ao single-coil")
                explanation = "Amaciei os agudos do seu som! Reduzi a presença e os agudos do amplificador, cortei 3.2kHz no EQ e ajustei o filtro da caixa (IR) para 6.5kHz. Agora sua Strato/Tele soa encorpada, aveludada e sem estridência."
            }

            // 2. Modo Solo / Mais ganho / Destacar / Sustentação
            query.contains("solo") || query.contains("ganho") || query.contains("lead") ||
            query.contains("sustent") || query.contains("destacar") || query.contains("mais drive") -> {
                ampParams["Gain"] = ((ampParams["Gain"] ?: 45f) + 18f).coerceAtMost(82f)
                ampParams["Middle"] = ((ampParams["Middle"] ?: 50f) + 8f).coerceAtMost(75f)
                dsParams["gain"] = 18f
                dsParams["level"] = 92f
                dsParams["tone"] = 56f
                dlyParams["Time"] = 380f
                dlyParams["Fb"] = 30f
                dlyParams["Mix"] = 24f
                eqParams["800Hz"] = (eqParams["800Hz"] ?: 0f) + 2f
                eqParams["1.6kHz"] = (eqParams["1.6kHz"] ?: 0f) + 3f
                newTitle = if (!newTitle.contains("Solo")) "$newTitle (Solo)" else newTitle
                changes.add("Ganho (+18) e Médios (+8) aumentados no amplificador")
                changes.add("Overdrive MW-808 ajustado como booster de harmônicos")
                changes.add("Delay analógico sincronizado em 380ms para sustentação")
                changes.add("Destaque melódico (+3dB em 1.6kHz) para cortar na mix")
                explanation = "Preparei seu timbre para modo Solo! Empurrei o ganho com o booster, aumentei os médios em 1.6kHz para sua guitarra cortar na frente da banda e ativei um delay analógico fluido para dar sustentação."
            }

            // 3. Refrão Pesado / Distorção / Metal / Peso
            query.contains("refrão") || query.contains("refrao") || query.contains("pesado") ||
            query.contains("distor") || query.contains("metal") || query.contains("peso") ||
            query.contains("pancada") || query.contains("high gain") -> {
                val modernAmp = "70OD2_meas" // Mesa Dual Rectifier moderno
                val modernCab = "46MbOS412A" // Mesa 4x12 OS
                ampParams["Gain"] = 68f
                ampParams["Bass"] = 58f
                ampParams["Middle"] = 56f
                ampParams["Treble"] = 64f
                ampParams["Pres"] = 58f
                dsParams["gain"] = 12f
                dsParams["level"] = 94f
                dsParams["tone"] = 55f
                eqParams["100Hz"] = 2f
                eqParams["400Hz"] = -2f
                eqParams["1.6kHz"] = 2f
                eqParams["3.2kHz"] = 1f
                newTitle = if (!newTitle.contains("Refrão")) "$newTitle (Refrão Pesado)" else newTitle
                changes.add("Amplificador trocado para Mesa Dual Rectifier (70OD2_meas)")
                changes.add("Gabinete trocado para 4x12 Mesa Oversized (46MbOS412A)")
                changes.add("Booster MW-808 ativo para aperto de palhetada nos graves")
                changes.add("Equalização com pegada de Rock moderno (+2dB 100Hz, -2dB 400Hz)")
                explanation = "Criei a parede de distorção para o refrão pesado! Configurei o Mesa Dual Rectifier com caixa 4x12, apertei a resposta dos graves com o booster MW-808 e limpei os médios embolados."
                val updatedPedals = pedals.copy(
                    AMP = pedals.AMP?.copy(model = modernAmp, params = ampParams)
                        ?: PedalModule(name = "Amplificador", active = true, model = modernAmp, params = ampParams),
                    CAB = pedals.CAB?.copy(model = modernCab, params = cabParams)
                        ?: PedalModule(name = "Caixa / IR", active = true, model = modernCab, params = cabParams),
                    DS = pedals.DS?.copy(active = true, model = "MW-808", params = dsParams)
                        ?: PedalModule(name = "Drive / Boost", active = true, model = "MW-808", params = dsParams),
                    EQ = pedals.EQ?.copy(params = eqParams),
                    NS = pedals.NS?.copy(active = true, model = "soft gate", params = mapOf("thd" to -58f, "inputDetect" to 0f))
                )
                val rawPreset = currentPreset.copy(title = newTitle, pedals = updatedPedals)
                val sanitized = com.example.util.AcousticStudioGuardrail.sanitize(rawPreset)
                return ToneAssistantResult(
                    explanation = explanation,
                    appliedChanges = changes,
                    updatedPreset = sanitized
                )
            }

            // 4. Menos Reverb / Menos Delay / Som Seco / Embolado
            query.contains("menos reverb") || query.contains("tirar reverb") || query.contains("seco") ||
            query.contains("embolado") || query.contains("menos delay") || query.contains("muito reverb") ||
            query.contains("exagerado") || query.contains("limpar") -> {
                revParams["mix"] = 14f
                revParams["decay"] = 18f
                dlyParams["Mix"] = 16f
                dlyParams["Fb"] = 20f
                cabParams["Low Cut"] = 85f
                eqParams["400Hz"] = -2f
                newTitle = if (!newTitle.contains("Firme")) "$newTitle (Firme/Seco)" else newTitle
                changes.add("Mix do Reverb reduzido para 14% (sensação natural de sala)")
                changes.add("Mix do Delay reduzido para 16% com feedback contido")
                changes.add("Low Cut do Gabinete limpo em 85Hz")
                changes.add("Corte de -2dB em 400Hz para eliminar embolamento")
                explanation = "Limpei a ambiência do seu som! Reduzi o Mix do Reverb para 14% e do Delay para 16%, cortei os subgraves embolados na caixa e reduzi 400Hz no EQ. O som agora está direto, firme e com ataque nítido na palhetada."
            }

            // 5. Mais Ambiência / Chorus / Shimmer / Espacial / Worship
            query.contains("chorus") || query.contains("shimmer") || query.contains("espacial") ||
            query.contains("ambiente") || query.contains("worship") || query.contains("mais reverb") -> {
                revParams["mix"] = 28f
                revParams["decay"] = 48f
                dlyParams["Mix"] = 25f
                dlyParams["Time"] = 420f
                dlyParams["Fb"] = 32f
                val modParams = pedals.MOD?.params?.toMutableMap() ?: mutableMapOf()
                modParams["mix"] = 32f
                modParams["speed"] = 1.2f
                modParams["depth"] = 40f
                newTitle = if (!newTitle.contains("Espacial")) "$newTitle (Espacial)" else newTitle
                changes.add("Chorus Estéreo ativado com modulação suave e envolvente")
                changes.add("Reverb Shimmer/Plate ampliado para Mix 28% e Decay 48")
                changes.add("Delay analógico prolongado para 420ms")
                explanation = "Adicionei uma rica atmosfera espacial! Ativei o Chorus estéreo suave, expandi o Reverb para 28% de Mix com cauda brilhante e ajustei o delay para 420ms. Perfeito para arpejos e dedilhados imersivos."
                val updatedPedals = pedals.copy(
                    MOD = pedals.MOD?.copy(active = true, model = "CHORUS STEREO", params = modParams)
                        ?: PedalModule(name = "Modulação", active = true, model = "CHORUS STEREO", params = modParams),
                    REVERB = pedals.REVERB?.copy(active = true, params = revParams),
                    DELAY = pedals.DELAY?.copy(active = true, params = dlyParams),
                    EQ = pedals.EQ?.copy(params = eqParams),
                    CAB = pedals.CAB?.copy(params = cabParams),
                    AMP = pedals.AMP?.copy(params = ampParams)
                )
                val rawPreset = currentPreset.copy(title = newTitle, pedals = updatedPedals)
                val sanitized = com.example.util.AcousticStudioGuardrail.sanitize(rawPreset)
                return ToneAssistantResult(
                    explanation = explanation,
                    appliedChanges = changes,
                    updatedPreset = sanitized
                )
            }

            // 6. Mais Graves / Pouco Grave / Encorpar
            query.contains("grave") || query.contains("corpo") || query.contains("fino") ||
            query.contains("magro") || query.contains("gordo") -> {
                ampParams["Bass"] = ((ampParams["Bass"] ?: 50f) + 12f).coerceAtMost(75f)
                eqParams["100Hz"] = (eqParams["100Hz"] ?: 0f) + 2f
                eqParams["200Hz"] = (eqParams["200Hz"] ?: 0f) + 1f
                cabParams["Low Cut"] = 70f
                newTitle = if (!newTitle.contains("Gordo")) "$newTitle (Encorpado)" else newTitle
                changes.add("Bass (+12) aumentado no amplificador")
                changes.add("Reforço de graves (+2dB em 100Hz) no Equalizador")
                changes.add("Low Cut da Caixa expandido para 70Hz")
                explanation = "Encorpei o timbre! Adicionei graves no amplificador, impulsionei 100Hz no EQ e abri o filtro de subgraves da caixa para dar peso e corpo à sua guitarra."
            }

            // Default / Refinamento Geral
            else -> {
                ampParams["Middle"] = ((ampParams["Middle"] ?: 50f) + 4f).coerceAtMost(70f)
                eqParams["800Hz"] = (eqParams["800Hz"] ?: 0f) + 1f
                cabParams["High Cut"] = 6800f
                cabParams["Low Cut"] = 80f
                changes.add("Médios calibrados para maior definição melódica")
                changes.add("Filtros do Gabinete otimizados (80Hz - 6.8kHz)")
                explanation = "Ajustei e calibrei seu timbre para máxima definição e clareza no palco e fones da MK-20! Os médios foram refinados e a caixa foi equalizada para soar equilibrada."
            }
        }

        val updatedPedals = pedals.copy(
            AMP = pedals.AMP?.copy(params = ampParams),
            CAB = pedals.CAB?.copy(params = cabParams),
            EQ = pedals.EQ?.copy(params = eqParams),
            DS = pedals.DS?.copy(params = dsParams),
            REVERB = pedals.REVERB?.copy(params = revParams),
            DELAY = pedals.DELAY?.copy(params = dlyParams)
        )

        val finalPreset = currentPreset.copy(
            title = newTitle,
            pedals = updatedPedals
        )

        val sanitized = com.example.util.AcousticStudioGuardrail.sanitize(finalPreset)
        return ToneAssistantResult(
            explanation = explanation,
            appliedChanges = changes,
            updatedPreset = sanitized
        )
    }
}
