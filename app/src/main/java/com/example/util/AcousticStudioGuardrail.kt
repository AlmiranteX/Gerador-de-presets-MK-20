package com.example.util

import com.example.data.model.Mk20Preset
import com.example.data.model.PedalModule

/**
 * Guardião Acústico de Estúdio (Acoustic Studio Guardrail) para a pedaleira M-VAVE MK-20.
 *
 * Implementa as regras físicas e harmônicas de engenharia de áudio para guitarra elétrica:
 * 1. Desacoplamento de Ganho (Gain Staging): Impede compressor ativo com drive/amp saturado (causa #1 de microfonia).
 * 2. Calibração Dinâmica do Noise Gate: Fecha o gate no nível certo conforme a saturação (elimina zumbido/chiado).
 * 3. Teto Anti-Ressonância de Agudos: Impede acúmulo de agudos e presença que geram apito.
 * 4. Polimento de Estúdio no Gabinete: Aplica Low Cut (75-85Hz) e High Cut (6.5k-7.2kHz) protegendo fones e PA.
 */
object AcousticStudioGuardrail {

    fun sanitize(preset: Mk20Preset): Mk20Preset {
        val pedals = preset.pedals

        val ampMod = pedals.AMP
        val dsMod = pedals.DS
        val fxMod = pedals.FX
        val nsMod = pedals.NS ?: pedals.NR
        val cabMod = pedals.CAB
        val eqMod = pedals.EQ
        val revMod = pedals.REVERB
        val dlyMod = pedals.DELAY

        val ampParams = ampMod?.params?.toMutableMap() ?: mutableMapOf()
        val dsParams = dsMod?.params?.toMutableMap() ?: mutableMapOf()
        val fxParams = fxMod?.params?.toMutableMap() ?: mutableMapOf()
        val nsParams = nsMod?.params?.toMutableMap() ?: mutableMapOf()
        val cabParams = cabMod?.params?.toMutableMap() ?: mutableMapOf()
        val eqParams = eqMod?.params?.toMutableMap() ?: mutableMapOf()
        val revParams = revMod?.params?.toMutableMap() ?: mutableMapOf()
        val dlyParams = dlyMod?.params?.toMutableMap() ?: mutableMapOf()

        val ampActive = ampMod?.active == true
        val ampGain = (ampParams["Gain"] ?: 50f).coerceIn(0f, 100f)
        val dsActive = dsMod?.active == true
        val dsGain = (dsParams["gain"] ?: dsParams["Gain"] ?: 0f).coerceIn(0f, 100f)
        val dsLevel = (dsParams["level"] ?: dsParams["Level"] ?: 50f).coerceIn(0f, 100f)

        // -------------------------------------------------------------
        // REGRA 1: GAIN STAGING & SUSTAIN (Equilíbrio Musical de Estúdio)
        // -------------------------------------------------------------
        var newFxActive = fxMod?.active == true
        val fxModel = fxMod?.model ?: ""
        val isCompressor = fxModel.contains("Compress", ignoreCase = true)

        if (isCompressor && newFxActive) {
            // Apenas desativa compressor se houver saturação extrema de 3 estágios (Amp > 62 e Drive > 45)
            if (ampActive && ampGain >= 62f && dsActive && dsGain >= 45f) {
                newFxActive = false
            } else {
                // Mantém compressor ativo com parâmetros musicais que dão corpo e brilho sem microfonia
                val currLevel = fxParams["Level"] ?: 70f
                val currSustain = fxParams["Sustain"] ?: 5.0f
                if (currLevel > 72f) fxParams["Level"] = 68f
                if (currSustain > 5.5f) fxParams["Sustain"] = 4.8f
            }
        }

        // Se o Overdrive estiver empurrando um amplificador já quente, equilibra para booster definido
        if (dsActive && ampActive && ampGain >= 55f) {
            if (dsGain > 30f) {
                dsParams["gain"] = 22f
                dsParams["Gain"] = 22f
            }
            if (dsLevel > 95f) {
                dsParams["level"] = 90f
                dsParams["Level"] = 90f
            }
        }

        // -------------------------------------------------------------
        // REGRA 2: CALIBRAÇÃO NATURAL DO NOISE GATE (Não engole o som / Não sufoca notas)
        // -------------------------------------------------------------
        val effectiveGain = (if (ampActive) ampGain else 0f) +
                (if (dsActive) (dsGain * 0.7f + (dsLevel - 50f).coerceAtLeast(0f) * 0.3f) else 0f)

        var newNsActive = true
        var newNsModel = nsMod?.model ?: "soft gate"

        when {
            // Alto Ganho Extremo / Metal (ampGain >= 65 e effectiveGain >= 90)
            ampGain >= 65f && effectiveGain >= 90f -> {
                nsParams["thd"] = -48f
                nsParams["inputDetect"] = 0f
            }
            // Crunch / Lead / Solo Melódico / Overdrive (45 <= effectiveGain)
            effectiveGain >= 45f -> {
                nsParams["thd"] = -52f
                nsParams["inputDetect"] = 0f
            }
            // Som Limpo / Dedilhado / Clean
            else -> {
                nsParams["thd"] = -58f
                nsParams["inputDetect"] = 0f
            }
        }

        // -------------------------------------------------------------
        // REGRA 3: PRESERVAÇÃO DE BRILHO E PRESENÇA OFICIAL (Chime & Harmônicos)
        // -------------------------------------------------------------
        val ampPres = ampParams["Pres"] ?: 50f
        val ampTreble = ampParams["Treble"] ?: 50f

        // Garante que o Bright Switch e a Presença respirem para dar o brilho oficial do timbre
        if (!ampParams.containsKey("Bright") || (ampParams["Bright"] ?: 0f) < 40f) {
            ampParams["Bright"] = 65f
        }

        // No Equalizador pós-gabinete (EQ):
        // Permite até +2dB em 1.6kHz e 3.2kHz para destacar a palhetada e dar o estalo melódico
        val eq32k = eqParams["3.2kHz"] ?: 0f
        val eq16k = eqParams["1.6kHz"] ?: 0f
        if (eq32k > 2.5f) eqParams["3.2kHz"] = 2f
        if (eq16k > 2.5f) eqParams["1.6kHz"] = 2f

        // No Gabinete (CAB):
        // High Cut em 7.5kHz a 8.2kHz (permite o brilho e o ar natural do alto-falante)
        val cabLowCutRaw = cabParams["Low Cut"] ?: 80f
        val cabHighCutRaw = cabParams["High Cut"] ?: 7500f

        val normalizedHighCut = if (cabHighCutRaw > 200f) cabHighCutRaw else cabHighCutRaw * 100f
        if (normalizedHighCut < 6000f) {
            cabParams["High Cut"] = if (cabHighCutRaw > 200f) 7800f else 78f
        } else if (normalizedHighCut > 9500f) {
            cabParams["High Cut"] = if (cabHighCutRaw > 200f) 8200f else 82f
        }

        // Low Cut para proteger contra estrondo de subgraves sem emagrecer o corpo
        val normalizedLowCut = cabLowCutRaw
        if (normalizedLowCut < 70f) {
            cabParams["Low Cut"] = 75f
        } else if (normalizedLowCut > 95f) {
            cabParams["Low Cut"] = 80f
        }

        // -------------------------------------------------------------
        // REGRA 4: EQUILÍBRIO DE AMBIÊNCIA (Anti-Embolamento de Reverb/Delay)
        // -------------------------------------------------------------
        val isAmbientStyle = preset.style.contains("Ambient", ignoreCase = true) ||
                preset.style.contains("Worship", ignoreCase = true) ||
                preset.title.contains("Shimmer", ignoreCase = true)

        val maxRevMix = if (isAmbientStyle) 30f else 22f
        val maxRevDecay = if (isAmbientStyle) 45f else 28f
        val currRevMix = revParams["mix"] ?: 20f
        val currRevDecay = revParams["decay"] ?: 22f

        if (currRevMix > maxRevMix) revParams["mix"] = maxRevMix
        if (currRevDecay > maxRevDecay) revParams["decay"] = maxRevDecay

        val currDlyMix = dlyParams["Mix"] ?: 20f
        val currDlyFb = dlyParams["Fb"] ?: 25f
        if (currDlyMix > 25f) dlyParams["Mix"] = 22f
        if (currDlyFb > 38f) dlyParams["Fb"] = 30f

        // Se ambos Reverb e Delay estiverem ativos em som com ganho alto, equilibra o mix
        if (pedals.REVERB?.active == true && pedals.DELAY?.active == true && effectiveGain >= 55f) {
            if ((revParams["mix"] ?: 0f) > 18f) revParams["mix"] = 18f
            if ((dlyParams["Mix"] ?: 0f) > 20f) dlyParams["Mix"] = 18f
        }

        // Monta o preset final calibrado
        val sanitizedPedals = pedals.copy(
            AMP = ampMod?.copy(params = ampParams),
            DS = dsMod?.copy(params = dsParams),
            FX = fxMod?.copy(active = newFxActive, params = fxParams),
            NS = (nsMod ?: PedalModule(name = "Noise Suppressor", active = true, model = newNsModel, params = nsParams))
                .copy(active = newNsActive, model = newNsModel, params = nsParams),
            CAB = cabMod?.copy(params = cabParams),
            EQ = eqMod?.copy(params = eqParams),
            REVERB = revMod?.copy(params = revParams),
            DELAY = dlyMod?.copy(params = dlyParams)
        )

        // Garante volume master oficial de fábrica da MK-20 (55..75) para não soar baixo
        val officialVolume = preset.masterVolume.coerceIn(55, 75)

        return preset.copy(masterVolume = officialVolume, pedals = sanitizedPedals)
    }
}
