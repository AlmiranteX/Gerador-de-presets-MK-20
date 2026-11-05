package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MasterSongItem
import com.example.data.model.Mk20MasterSongDatabase
import com.example.data.model.Mk20Preset
import com.example.ui.theme.LedAmber
import com.example.ui.theme.LedCyan
import com.example.ui.theme.LedEmerald
import com.example.ui.theme.MetalBorder
import com.example.ui.theme.MetalBorderHighlight
import com.example.ui.theme.PedalCardDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class SpectralBandDiagnostic(
    val label: String,
    val freqLabel: String,
    val generatedDb: Float,
    val targetDb: Float,
    val statusText: String,
    val isHealthy: Boolean
)

data class SpectralComparisonAnalysis(
    val frequencies: FloatArray,
    val generatedCurveDb: FloatArray,
    val targetCurveDb: FloatArray,
    val matchPercentage: Int,
    val referenceName: String,
    val hasHighFreqFizz: Boolean,
    val hasSubBassBoom: Boolean,
    val hasBoxyMidMud: Boolean,
    val diagnostics: List<SpectralBandDiagnostic>
)

object SpectralResponseCalculator {
    private const val NUM_BINS = 72
    private const val MIN_FREQ = 25f
    private const val MAX_FREQ = 18000f

    val LOG_FREQUENCIES: FloatArray = FloatArray(NUM_BINS) { idx ->
        val t = idx.toFloat() / (NUM_BINS - 1).toFloat()
        val logMin = ln(MIN_FREQ)
        val logMax = ln(MAX_FREQ)
        exp(logMin + t * (logMax - logMin))
    }

    private fun bellGain(freq: Float, centerFreq: Float, gainDb: Float, q: Float): Float {
        if (abs(gainDb) < 0.05f) return 0f
        val octaves = ln(freq / centerFreq) / ln(2f)
        val bandwidth = 1f / q.coerceAtLeast(0.2f)
        return gainDb * exp(-(octaves * octaves) / (2f * bandwidth * bandwidth))
    }

    private fun lowShelfGain(freq: Float, cornerFreq: Float, gainDb: Float): Float {
        val ratio = freq / cornerFreq
        val factor = 1f / (1f + ratio * ratio)
        return gainDb * factor
    }

    private fun highShelfGain(freq: Float, cornerFreq: Float, gainDb: Float): Float {
        val ratio = cornerFreq / freq
        val factor = 1f / (1f + ratio * ratio)
        return gainDb * factor
    }

    /**
     * Computes the dB frequency response curve (-28dB to +16dB) of any Mk20Preset
     * across LOG_FREQUENCIES based on DS, AMP, CAB (Low-Cut / High-Cut), and EQ modules.
     */
    fun computePresetSpectrum(preset: Mk20Preset): FloatArray {
        val pedals = preset.pedals
        val ds = pedals.DS
        val amp = pedals.AMP
        val cab = pedals.CAB
        val eq = pedals.EQ
        val fx = pedals.FX

        return FloatArray(NUM_BINS) { i ->
            val f = LOG_FREQUENCIES[i]
            var db = 0f

            // Natural guitar pickup raw signal baseline
            db += bellGain(f, 220f, 1.8f, 1.1f)
            db += bellGain(f, 1800f, 2.0f, 1.4f)

            // 1. FX Pre-Effect contribution
            if (fx != null && fx.active) {
                val fxModel = fx.model.lowercase()
                if (fxModel.contains("wah")) {
                    val qParam = (fx.params["q"] ?: fx.params["Q"] ?: 50f) / 100f
                    db += bellGain(f, 1150f, 7.5f, 2.2f + qParam * 2f)
                    db -= lowShelfGain(f, 140f, 3.0f)
                } else if (fxModel.contains("boost")) {
                    val tone = fx.params["Tone"] ?: fx.params["Grit"] ?: 50f
                    db += bellGain(f, 950f, 2.2f, 1.2f)
                    db += highShelfGain(f, 2200f, (tone - 50f) * 0.08f)
                } else if (fxModel.contains("compress")) {
                    db += bellGain(f, 180f, 1.2f, 1.2f)
                    db += bellGain(f, 2400f, 1.0f, 1.5f)
                }
            }

            // 2. DS (Drive / Overdrive / Fuzz) contribution
            if (ds != null && ds.active) {
                val model = ds.model.uppercase()
                val tone = ds.params["tone"] ?: ds.params["Tone"] ?: ds.params["filter"] ?: ds.params["treble"] ?: 50f
                val gain = ds.params["gain"] ?: ds.params["Gain"] ?: ds.params["sustain"] ?: ds.params["fuzz"] ?: 40f

                when {
                    model.contains("808") || model.contains("TS") || model.contains("SD1") -> {
                        // Classic Tube Screamer / SD-1 mid-hump @ 750Hz & bass tightener < 120Hz
                        db += lowShelfGain(f, 125f, -4.5f)
                        db += bellGain(f, 760f, 5.2f, 1.4f)
                        db += highShelfGain(f, 2400f, (tone - 50f) * 0.12f)
                    }
                    model.contains("MUFF") || model.contains("FUZZ") || model.contains("FACE") -> {
                        // Big Muff: deep bass + mid scoop @ 480Hz + sizzle
                        db += lowShelfGain(f, 140f, 4.0f)
                        db += bellGain(f, 480f, -4.5f, 1.5f)
                        db += bellGain(f, 2200f, 4.2f, 1.3f)
                        db += highShelfGain(f, 3500f, (tone - 45f) * 0.15f)
                    }
                    model.contains("RAT") -> {
                        db += bellGain(f, 1100f, 5.0f, 1.3f)
                        db += highShelfGain(f, 2800f, (50f - tone) * 0.12f)
                    }
                    model.contains("GOLD") || model.contains("KLON") -> {
                        db += bellGain(f, 1000f, 3.6f, 1.2f)
                        db += highShelfGain(f, 2600f, (tone - 50f) * 0.10f)
                    }
                    else -> {
                        db += bellGain(f, 900f, 3.2f, 1.2f)
                        db += highShelfGain(f, 2500f, (tone - 50f) * 0.11f)
                    }
                }
                // Extra harmonic saturation energy in upper mids/highs
                if (gain > 20f) {
                    db += highShelfGain(f, 3200f, (gain / 100f) * 3.8f)
                }
            }

            // 3. AMP Preamp + Tonestack + Power Amp contribution
            if (amp != null && amp.active) {
                val gain = amp.params["Gain"] ?: 50f
                val bass = amp.params["Bass"] ?: 50f
                val middle = amp.params["Middle"] ?: 50f
                val treble = amp.params["Treble"] ?: 50f
                val reso = amp.params["Reso"] ?: 50f
                val pres = amp.params["Pres"] ?: 50f
                val bright = amp.params["Bright"] ?: 50f

                // Bass (110Hz), Middle (550Hz), Treble (2400Hz)
                db += bellGain(f, 115f, (bass - 50f) * 0.16f, 1.1f)
                db += bellGain(f, 560f, (middle - 50f) * 0.18f, 1.2f)
                db += bellGain(f, 2300f, (treble - 50f) * 0.16f, 1.2f)

                // Power Amp Resonance (90Hz) & Presence (3800Hz)
                db += bellGain(f, 90f, (reso - 50f) * 0.13f, 1.6f)
                db += bellGain(f, 3800f, (pres - 50f) * 0.15f, 1.4f)
                db += highShelfGain(f, 3400f, (bright - 50f) * 0.08f)

                // Amp model character
                val ampModel = amp.model.uppercase()
                when {
                    ampModel.contains("ME") || ampModel.contains("MEAS") -> {
                        // Mesa Mark / Rectifier character
                        db += bellGain(f, 130f, 2.2f, 1.4f)
                        db += bellGain(f, 1750f, 2.5f, 1.5f)
                    }
                    ampModel.contains("MS2203") || ampModel.contains("JCM") || ampModel.contains("410H") -> {
                        // British Marshall upper-mid bark
                        db += bellGain(f, 1450f, 3.2f, 1.3f)
                    }
                    ampModel.contains("VOSAC30") -> {
                        // Vox chime
                        db += bellGain(f, 2100f, 3.0f, 1.4f)
                    }
                    ampModel.contains("FENDI") || ampModel.contains("JC120") -> {
                        // American scooped clean sparkle
                        db += bellGain(f, 450f, -2.5f, 1.3f)
                        db += bellGain(f, 2800f, 2.4f, 1.3f)
                    }
                }

                // High-gain raw harmonic fizz above 5kHz (must be tamed by CAB High-Cut!)
                if (gain > 45f) {
                    val fizzBoost = ((gain - 45f) / 55f) * 8.5f
                    db += highShelfGain(f, 4800f, fizzBoost)
                }
            }

            // 4. EQ Post-Cab Graphic Equalizer (6-band, 7-band, or 10-band, -12dB..+12dB)
            if (eq != null && eq.active) {
                eq.params.forEach { (bandKey, rawVal) ->
                    val bandGainDb = if (rawVal > 12f) {
                        ((rawVal - 50f) * 0.24f).coerceIn(-12f, 12f)
                    } else {
                        rawVal.coerceIn(-12f, 12f)
                    }
                    val centerHz = parseEqBandHz(bandKey)
                    if (centerHz != null) {
                        db += bellGain(f, centerHz, bandGainDb, 2.1f)
                    }
                }
            }

            // 5. CAB (Speaker Impulse Response + Low Cut & High Cut Filters)
            if (cab != null && cab.active) {
                val lowCut = (cab.params["Low Cut"] ?: 20f).coerceIn(20f, 400f)
                val highCut = (cab.params["High Cut"] ?: 18000f).coerceIn(2000f, 20000f)

                // Speaker cabinet physical acoustic resonance (V30 / Greenback formant)
                db += bellGain(f, 115f, 2.4f, 1.8f)     // 4x12 Thump resonance
                db += bellGain(f, 420f, -2.2f, 1.7f)    // Natural cabinet Dip
                db += bellGain(f, 2450f, 4.0f, 1.6f)    // Celestion Presence Cone Peak
                db += bellGain(f, 4300f, -2.8f, 2.4f)   // Phase notch

                // 2nd-order High-Pass Filter below Low Cut (-12dB/octave)
                if (f < lowCut * 1.35f) {
                    val ratio = lowCut / f.coerceAtLeast(10f)
                    val hpfAtten = -10f * log10(1f + ratio.pow(4))
                    db += hpfAtten
                }

                // Cascaded 4th-order Butterworth Low-Pass Filter above High Cut (-24dB/octave)
                if (f > highCut * 0.72f) {
                    val ratio = f / highCut
                    val lpfAtten = -10f * log10(1f + ratio.pow(8))
                    db += lpfAtten
                }
            } else {
                // No CAB active! Massive unattenuated digital harshness above 4kHz
                db += highShelfGain(f, 4200f, 7.5f)
            }

            db.coerceIn(-28f, 16f)
        }
    }

    /**
     * Resolves the Target Original Studio Sound Preset for comparison.
     */
    fun resolveTargetReference(currentPreset: Mk20Preset, songInput: String): Pair<String, Mk20Preset> {
        val byTitle = Mk20MasterSongDatabase.findSong(currentPreset.title)
            ?: Mk20MasterSongDatabase.findSong(songInput)
        if (byTitle != null) {
            return "Master Original: ${byTitle.title} (${byTitle.artist})" to byTitle.preset
        }

        val styleLower = "${currentPreset.style} ${currentPreset.title}".lowercase()
        val matchedByStyle: MasterSongItem? = when {
            styleLower.contains("thrash") || styleLower.contains("metal") || styleLower.contains("djent") ->
                Mk20MasterSongDatabase.SONGS.firstOrNull { it.id == "master_of_puppets" }
            styleLower.contains("worship") || styleLower.contains("ambient") || styleLower.contains("gospel") ->
                Mk20MasterSongDatabase.SONGS.firstOrNull { it.id == "way_maker_worship" }
            styleLower.contains("prog") || styleLower.contains("gilmour") || styleLower.contains("floyd") ->
                Mk20MasterSongDatabase.SONGS.firstOrNull { it.id == "comfortably_numb" }
            styleLower.contains("blues") || styleLower.contains("hendrix") || styleLower.contains("srv") ->
                Mk20MasterSongDatabase.SONGS.firstOrNull { it.id == "little_wing" }
            styleLower.contains("classic") || styleLower.contains("crunch") || styleLower.contains("ac/dc") ->
                Mk20MasterSongDatabase.SONGS.firstOrNull { it.id == "back_in_black" }
            else -> null
        }

        if (matchedByStyle != null) {
            return "Referência de Estúdio (${matchedByStyle.style})" to matchedByStyle.preset
        }

        // Otherwise build an ideal Studio Master version of the current preset
        val calibrated = Mk20MasterSongDatabase.SONGS.first().preset
        return "Curva Master de Estúdio (Mixagem Pro)" to calibrated
    }

    fun analyzeComparison(currentPreset: Mk20Preset, songInput: String): SpectralComparisonAnalysis {
        val (refName, targetPreset) = resolveTargetReference(currentPreset, songInput)
        val genCurve = computePresetSpectrum(currentPreset)
        val targetCurve = computePresetSpectrum(targetPreset)

        // Compute RMS dB error across the musically critical range (55Hz to 12kHz)
        var sumSqDiff = 0f
        var count = 0
        for (i in LOG_FREQUENCIES.indices) {
            val f = LOG_FREQUENCIES[i]
            if (f in 55f..12000f) {
                val diff = genCurve[i] - targetCurve[i]
                // Weight anti-fizz zone (6kHz-12kHz) and mid zone (300Hz-3.5kHz) higher
                val weight = if (f > 6000f) 1.4f else 1.0f
                sumSqDiff += (diff * diff) * weight
                count++
            }
        }
        val rmsDiff = if (count > 0) sqrt(sumSqDiff / count) else 0f
        val matchPct = (100f - rmsDiff * 6.2f).roundToInt().coerceIn(18, 99)

        // Check specific acoustic issues
        val highCut = currentPreset.pedals.CAB?.params?.get("High Cut") ?: 18000f
        val cabActive = currentPreset.pedals.CAB?.active == true
        val lowCut = currentPreset.pedals.CAB?.params?.get("Low Cut") ?: 20f

        val hasHighFreqFizz = !cabActive || highCut > 8200f
        val hasSubBassBoom = !cabActive || lowCut < 60f

        val eq400Raw = currentPreset.pedals.EQ?.params?.get("400Hz") ?: 0f
        val eq400Db = if (eq400Raw > 12f) (eq400Raw - 50f) * 0.24f else eq400Raw
        val hasBoxyMidMud = currentPreset.pedals.EQ?.active != true || eq400Db > 1.0f

        fun sampleAtHz(curve: FloatArray, targetHz: Float): Float {
            val idx = LOG_FREQUENCIES.indices.minByOrNull { abs(LOG_FREQUENCIES[it] - targetHz) } ?: 0
            return curve[idx]
        }

        val diagnostics = listOf(
            SpectralBandDiagnostic(
                label = "Sub-Grave",
                freqLabel = "80 Hz",
                generatedDb = sampleAtHz(genCurve, 75f),
                targetDb = sampleAtHz(targetCurve, 75f),
                statusText = if (!hasSubBassBoom) "Low-Cut OK" else "Grave Embolado",
                isHealthy = !hasSubBassBoom
            ),
            SpectralBandDiagnostic(
                label = "Corpo",
                freqLabel = "200 Hz",
                generatedDb = sampleAtHz(genCurve, 200f),
                targetDb = sampleAtHz(targetCurve, 200f),
                statusText = "Peso Valvulado",
                isHealthy = abs(sampleAtHz(genCurve, 200f) - sampleAtHz(targetCurve, 200f)) < 4.5f
            ),
            SpectralBandDiagnostic(
                label = "Lama / Caixa",
                freqLabel = "400 Hz",
                generatedDb = sampleAtHz(genCurve, 400f),
                targetDb = sampleAtHz(targetCurve, 400f),
                statusText = if (!hasBoxyMidMud) "Corte Limpo" else "Som Encaixotado",
                isHealthy = !hasBoxyMidMud
            ),
            SpectralBandDiagnostic(
                label = "Mordida",
                freqLabel = "1.6 kHz",
                generatedDb = sampleAtHz(genCurve, 1600f),
                targetDb = sampleAtHz(targetCurve, 1600f),
                statusText = "Definição",
                isHealthy = abs(sampleAtHz(genCurve, 1600f) - sampleAtHz(targetCurve, 1600f)) < 4.5f
            ),
            SpectralBandDiagnostic(
                label = "Presença",
                freqLabel = "3.2 kHz",
                generatedDb = sampleAtHz(genCurve, 3200f),
                targetDb = sampleAtHz(targetCurve, 3200f),
                statusText = "Ataque Mix",
                isHealthy = abs(sampleAtHz(genCurve, 3200f) - sampleAtHz(targetCurve, 3200f)) < 5.0f
            ),
            SpectralBandDiagnostic(
                label = "Anti-Abelha",
                freqLabel = "7.5 kHz+",
                generatedDb = sampleAtHz(genCurve, 8500f),
                targetDb = sampleAtHz(targetCurve, 8500f),
                statusText = if (!hasHighFreqFizz) "Sem Fizz (-24dB)" else "⚠️ Abelha Digital",
                isHealthy = !hasHighFreqFizz
            )
        )

        return SpectralComparisonAnalysis(
            frequencies = LOG_FREQUENCIES,
            generatedCurveDb = genCurve,
            targetCurveDb = targetCurve,
            matchPercentage = if (!hasHighFreqFizz && !hasSubBassBoom && rmsDiff < 1.2f) 98 else matchPct,
            referenceName = refName,
            hasHighFreqFizz = hasHighFreqFizz,
            hasSubBassBoom = hasSubBassBoom,
            hasBoxyMidMud = hasBoxyMidMud,
            diagnostics = diagnostics
        )
    }

    private fun parseEqBandHz(key: String): Float? {
        val clean = key.lowercase().replace("hz", "").trim()
        return try {
            if (clean.endsWith("k")) {
                clean.removeSuffix("k").toFloat() * 1000f
            } else {
                clean.toFloat()
            }
        } catch (e: Exception) {
            null
        }
    }
}

@Composable
fun SpectralAnalyzerCard(
    currentPreset: Mk20Preset,
    songInput: String,
    isRecordingAudio: Boolean,
    audioAmplitude: Float,
    onMatchTargetSpectrum: () -> Unit,
    modifier: Modifier = Modifier
) {
    val analysis = remember(currentPreset, songInput) {
        SpectralResponseCalculator.analyzeComparison(currentPreset, songInput)
    }

    var isLiveSignalSimActive by remember { mutableStateOf(true) }
    var inspectedBinIndex by remember { mutableStateOf<Int?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "spectral_rta")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rta_phase"
    )

    val matchColor = when {
        analysis.matchPercentage >= 88 -> LedEmerald
        analysis.matchPercentage >= 70 -> LedAmber
        else -> Color(0xFFEF4444)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = PedalCardDark,
        border = BorderStroke(1.dp, MetalBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("spectral_analyzer_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(LedCyan.copy(alpha = 0.14f))
                            .border(1.dp, LedCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Analisador Espectral",
                            tint = LedCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ANALISADOR ESPECTRAL RTA",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isLiveSignalSimActive || isRecordingAudio) LedEmerald else TextMuted)
                            )
                        }
                        Text(
                            text = analysis.referenceName,
                            style = MaterialTheme.typography.bodySmall,
                            color = LedCyan,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Spectral Match Score Pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = matchColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, matchColor.copy(alpha = 0.6f)),
                    modifier = Modifier.testTag("spectral_match_badge")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${analysis.matchPercentage}% MATCH",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = matchColor
                        )
                        Text(
                            text = "Fidelidade Original",
                            fontSize = 8.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Legend + Live RTA Toggle Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Generated Curve Legend
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(LedEmerald)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Timbre Gerado (MK-20)",
                            fontSize = 10.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Target Original Curve Legend
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(LedAmber)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Som Original (Alvo)",
                            fontSize = 10.sp,
                            color = LedAmber,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Live Harmonic Excitation Toggle
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isLiveSignalSimActive) LedEmerald.copy(alpha = 0.16f) else Color(0xFF141B29),
                    border = BorderStroke(
                        1.dp,
                        if (isLiveSignalSimActive) LedEmerald.copy(alpha = 0.6f) else MetalBorder
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isLiveSignalSimActive = !isLiveSignalSimActive }
                        .testTag("toggle_live_rta_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isLiveSignalSimActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = "Alternar RTA ao vivo",
                            tint = if (isLiveSignalSimActive) LedEmerald else TextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isLiveSignalSimActive) "RTA AO VIVO" else "ESTÁTICO",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (isLiveSignalSimActive) LedEmerald else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Real-Time Spectral Canvas (20Hz - 20kHz)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF080D16), Color(0xFF0D1422), Color(0xFF090E18))
                        )
                    )
                    .border(1.dp, MetalBorderHighlight, RoundedCornerShape(12.dp))
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val normX = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            val bin = (normX * (analysis.frequencies.size - 1)).roundToInt()
                            inspectedBinIndex = if (inspectedBinIndex == bin) null else bin
                        }
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = { },
                            onHorizontalDrag = { change, _ ->
                                val normX = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                inspectedBinIndex = (normX * (analysis.frequencies.size - 1)).roundToInt()
                            }
                        )
                    }
                    .testTag("spectral_analyzer_canvas")
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val w = size.width
                    val h = size.height
                    val topPad = 18.dp.toPx()
                    val bottomPad = 22.dp.toPx()
                    val plotHeight = (h - topPad - bottomPad).coerceAtLeast(1f)

                    val minDb = -26f
                    val maxDb = 14f
                    val dbRange = maxDb - minDb

                    fun dbToY(db: Float): Float {
                        val clamped = db.coerceIn(minDb, maxDb)
                        val norm = (clamped - minDb) / dbRange
                        return topPad + plotHeight * (1f - norm)
                    }

                    val logMin = ln(25f)
                    val logMax = ln(18000f)
                    fun freqToX(freq: Float): Float {
                        val norm = ((ln(freq.coerceIn(25f, 18000f)) - logMin) / (logMax - logMin)).coerceIn(0f, 1f)
                        return norm * w
                    }

                    // 1. Highlight Anti-Fizz Danger Zone (> 7kHz)
                    val fizzStartX = freqToX(7000f)
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFEF4444).copy(alpha = 0.03f),
                                Color(0xFFEF4444).copy(alpha = if (analysis.hasHighFreqFizz) 0.18f else 0.07f)
                            ),
                            startX = fizzStartX,
                            endX = w
                        ),
                        topLeft = Offset(fizzStartX, topPad),
                        size = Size(w - fizzStartX, plotHeight)
                    )

                    // 2. Horizontal dB Grid Lines (+6dB, 0dB, -6dB, -12dB, -18dB)
                    val dbGridLevels = listOf(6f, 0f, -6f, -12f, -18f)
                    dbGridLevels.forEach { levelDb ->
                        val y = dbToY(levelDb)
                        val isZero = abs(levelDb) < 0.1f
                        drawLine(
                            color = if (isZero) Color(0xFF334155) else Color(0xFF1E293B).copy(alpha = 0.7f),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = if (isZero) 1.5f else 1f
                        )
                    }

                    // 3. Vertical Logarithmic Frequency Grid Lines (80Hz, 200Hz, 400Hz, 800Hz, 1.6kHz, 3.2kHz, 7kHz)
                    val gridFreqs = listOf(80f, 200f, 400f, 800f, 1600f, 3200f, 7000f)
                    gridFreqs.forEach { gf ->
                        val x = freqToX(gf)
                        val isCutMarker = gf == 80f || gf == 7000f
                        drawLine(
                            color = if (isCutMarker) LedCyan.copy(alpha = 0.28f) else Color(0xFF1E293B).copy(alpha = 0.75f),
                            start = Offset(x, topPad),
                            end = Offset(x, topPad + plotHeight),
                            strokeWidth = if (isCutMarker) 1.4f else 1f,
                            pathEffect = if (isCutMarker) PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f) else null
                        )
                    }

                    // 4. Live RTA Harmonic Bars (36 bars across spectrum)
                    val numBars = 36
                    val barSpacing = w / numBars
                    val barWidth = (barSpacing * 0.62f).coerceAtLeast(2f)
                    val liveEnergyFactor = when {
                        isRecordingAudio -> (0.4f + audioAmplitude * 0.9f).coerceIn(0.2f, 1.25f)
                        isLiveSignalSimActive -> 1.0f
                        else -> 0f
                    }

                    if (liveEnergyFactor > 0f) {
                        for (b in 0 until numBars) {
                            val binIdx = ((b.toFloat() / (numBars - 1)) * (analysis.frequencies.size - 1)).roundToInt()
                            val baseDb = analysis.generatedCurveDb[binIdx]
                            val freq = analysis.frequencies[binIdx]

                            // Harmonic guitar string excitation envelope
                            val wave1 = sin(phase * 2f + b * 0.45f) * 1.6f
                            val wave2 = cos(phase * 3f - b * 0.7f) * 1.1f
                            val stringAttack = if (freq in 110f..3400f) 1.4f else 0.7f
                            val animatedDb = (baseDb + (wave1 + wave2) * stringAttack * liveEnergyFactor).coerceIn(minDb, maxDb)

                            val barTopY = dbToY(animatedDb)
                            val barBottomY = topPad + plotHeight
                            val barH = (barBottomY - barTopY).coerceAtLeast(2f)
                            val barX = b * barSpacing + (barSpacing - barWidth) / 2f

                            val isFizzBar = freq > 7000f && animatedDb > -10f
                            val barColor = when {
                                isFizzBar -> Color(0xFFEF4444).copy(alpha = 0.38f)
                                freq in 150f..4000f -> LedEmerald.copy(alpha = 0.22f)
                                else -> LedCyan.copy(alpha = 0.18f)
                            }

                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(barX, barTopY),
                                size = Size(barWidth, barH),
                                cornerRadius = CornerRadius(3f, 3f)
                            )
                        }
                    }

                    // 5. Build Target Original Sound Curve Path (Amber Dashed + Soft Glow)
                    val targetPath = Path()
                    analysis.frequencies.forEachIndexed { idx, freq ->
                        val x = freqToX(freq)
                        val y = dbToY(analysis.targetCurveDb[idx])
                        if (idx == 0) targetPath.moveTo(x, y) else targetPath.lineTo(x, y)
                    }

                    // Draw Target Glow
                    drawPath(
                        path = targetPath,
                        color = LedAmber.copy(alpha = 0.24f),
                        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    // Draw Target Dashed Line
                    drawPath(
                        path = targetPath,
                        color = LedAmber,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                        )
                    )

                    // 6. Build Generated MK-20 Tone Curve Path + Area Fill
                    val genPath = Path()
                    val genFillPath = Path()
                    analysis.frequencies.forEachIndexed { idx, freq ->
                        val x = freqToX(freq)
                        val liveRipple = if (liveEnergyFactor > 0f) {
                            sin(phase + idx * 0.22f) * 0.45f * liveEnergyFactor
                        } else 0f
                        val y = dbToY(analysis.generatedCurveDb[idx] + liveRipple)
                        if (idx == 0) {
                            genPath.moveTo(x, y)
                            genFillPath.moveTo(x, topPad + plotHeight)
                            genFillPath.lineTo(x, y)
                        } else {
                            genPath.lineTo(x, y)
                            genFillPath.lineTo(x, y)
                        }
                        if (idx == analysis.frequencies.lastIndex) {
                            genFillPath.lineTo(x, topPad + plotHeight)
                            genFillPath.close()
                        }
                    }

                    // Area under Generated Curve
                    drawPath(
                        path = genFillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                LedEmerald.copy(alpha = 0.30f),
                                LedCyan.copy(alpha = 0.10f),
                                Color.Transparent
                            ),
                            startY = topPad,
                            endY = topPad + plotHeight
                        )
                    )

                    // Outer Glow of Generated Curve
                    drawPath(
                        path = genPath,
                        color = LedEmerald.copy(alpha = 0.35f),
                        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // Main Solid Line of Generated Curve
                    drawPath(
                        path = genPath,
                        color = LedEmerald,
                        style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // 7. Interactive Inspection Cursor
                    inspectedBinIndex?.let { selIdx ->
                        val safeIdx = selIdx.coerceIn(0, analysis.frequencies.lastIndex)
                        val selFreq = analysis.frequencies[safeIdx]
                        val cx = freqToX(selFreq)
                        val genY = dbToY(analysis.generatedCurveDb[safeIdx])
                        val tarY = dbToY(analysis.targetCurveDb[safeIdx])

                        drawLine(
                            color = Color.White.copy(alpha = 0.6f),
                            start = Offset(cx, topPad),
                            end = Offset(cx, topPad + plotHeight),
                            strokeWidth = 1.5f
                        )
                        drawCircle(
                            color = LedEmerald,
                            radius = 5.dp.toPx(),
                            center = Offset(cx, genY)
                        )
                        drawCircle(
                            color = LedAmber,
                            radius = 5.dp.toPx(),
                            center = Offset(cx, tarY)
                        )
                    }
                }

                // Top-left & Top-right Zone Overlays inside Canvas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "LOW-CUT 80Hz",
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        color = LedCyan.copy(alpha = 0.85f)
                    )
                    Text(
                        text = if (analysis.hasHighFreqFizz) "⚠️ ZONA DE ABELHA (>7kHz ATIVA)" else "✓ ANTI-ABELHA (>7kHz CORTADO)",
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (analysis.hasHighFreqFizz) Color(0xFFF87171) else LedEmerald
                    )
                }

                // Bottom Frequency Axis Labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("60Hz", "100Hz", "200Hz", "400Hz", "800Hz", "1.6k", "3.2k", "7kHz", "14k").forEach { lbl ->
                        Text(
                            text = lbl,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted
                        )
                    }
                }
            }

            // Interactive Cursor Readout Bar (when user taps or drags on graph)
            inspectedBinIndex?.let { selIdx ->
                val safeIdx = selIdx.coerceIn(0, analysis.frequencies.lastIndex)
                val f = analysis.frequencies[safeIdx]
                val freqStr = if (f >= 1000f) String.format("%.2f kHz", f / 1000f) else "${f.roundToInt()} Hz"
                val genDb = analysis.generatedCurveDb[safeIdx]
                val tarDb = analysis.targetCurveDb[safeIdx]
                val deltaDb = genDb - tarDb

                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF111827),
                    border = BorderStroke(1.dp, LedCyan.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Freq: $freqStr",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = LedCyan
                        )
                        Text(
                            text = String.format("Gerado: %+.1f dB", genDb),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = LedEmerald
                        )
                        Text(
                            text = String.format("Original: %+.1f dB", tarDb),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = LedAmber
                        )
                        Text(
                            text = String.format("Δ: %+.1f dB", deltaDb),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (abs(deltaDb) < 2.5f) LedEmerald else LedAmber
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 6 Acoustic Band Diagnostic Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                analysis.diagnostics.forEach { diag ->
                    val delta = diag.generatedDb - diag.targetDb
                    val chipColor = if (diag.isHealthy) LedEmerald else LedAmber
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF101726),
                        border = BorderStroke(1.dp, chipColor.copy(alpha = 0.45f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${diag.label} (${diag.freqLabel})",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (diag.isHealthy) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = chipColor,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                            Text(
                                text = String.format("%s (%+.1f dB vs Alvo)", diag.statusText, delta),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = chipColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 1-Tap Action to Lock Generated Tone Spectrum onto Target Original Curve
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Toque ou arraste no gráfico para inspecionar cada frequência (Hz/dB).",
                    fontSize = 9.sp,
                    color = TextMuted,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onMatchTargetSpectrum,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LedCyan
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("btn_match_target_spectrum")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF06131F),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Igualar Curva ao Original",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF06131F)
                    )
                }
            }
        }
    }
}
