package com.example.util

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt

data class AudioVibeAnalysis(
    val title: String,
    val detectedStyle: String,
    val gainVibe: String,
    val estimatedBpm: Int,
    val suggestedAmp: String,
    val suggestedCab: String,
    val suggestedDrive: String,
    val suggestedMod: String,
    val suggestedDelay: String,
    val suggestedReverb: String,
    val noiseGateActive: Boolean,
    val details: String,
    val rmsEnergy: Float,
    val highFreqRatio: Float,
    val base64Wav: String
)

class AudioVibeRecorder {

    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioRecord: AudioRecord? = null
    @Volatile
    private var isRecording = false

    @SuppressLint("MissingPermission")
    suspend fun recordAudio(
        maxDurationSeconds: Int = 5,
        onAmplitude: (Float) -> Unit,
        onSecondsUpdate: (Int) -> Unit
    ): ByteArray = withContext(Dispatchers.IO) {
        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            .coerceAtLeast(SAMPLE_RATE * 2)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )
        } catch (e: Exception) {
            return@withContext ByteArray(0)
        }

        val outputStream = ByteArrayOutputStream()
        val readBuffer = ShortArray(1024)
        val byteBuffer = ByteBuffer.allocate(readBuffer.size * 2).order(ByteOrder.LITTLE_ENDIAN)

        try {
            audioRecord?.startRecording()
            isRecording = true
            val startTime = System.currentTimeMillis()
            var lastSecondReported = 0

            while (isRecording && (System.currentTimeMillis() - startTime) < maxDurationSeconds * 1000L) {
                val readShorts = audioRecord?.read(readBuffer, 0, readBuffer.size) ?: 0
                if (readShorts > 0) {
                    byteBuffer.clear()
                    var sumSquare = 0.0
                    for (i in 0 until readShorts) {
                        val sample = readBuffer[i]
                        byteBuffer.putShort(sample)
                        sumSquare += sample * sample
                    }
                    outputStream.write(byteBuffer.array(), 0, readShorts * 2)

                    // Normalized amplitude for live visualizer (0.0 to 1.0)
                    val rms = sqrt(sumSquare / readShorts).toFloat()
                    val normalizedAmp = (rms / 8000f).coerceIn(0f, 1f)
                    withContext(Dispatchers.Main) {
                        onAmplitude(normalizedAmp)
                    }

                    val elapsedSeconds = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                    if (elapsedSeconds != lastSecondReported) {
                        lastSecondReported = elapsedSeconds
                        withContext(Dispatchers.Main) {
                            onSecondsUpdate(elapsedSeconds)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore capture interruption
        } finally {
            stop()
        }

        outputStream.toByteArray()
    }

    fun stop() {
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // Ignore release errors
        } finally {
            audioRecord = null
        }
    }

    fun pcmToWav(pcmData: ByteArray, sampleRate: Int = SAMPLE_RATE): ByteArray {
        val totalAudioLen = pcmData.size
        val totalDataLen = totalAudioLen + 36
        val channels = 1
        val byteRate = sampleRate * channels * 2

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // Subchunk1Size for PCM
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // AudioFormat 1 = PCM
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * 2).toByte() // block align
        header[33] = 0
        header[34] = 16 // bits per sample
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

        val wavData = ByteArray(44 + pcmData.size)
        System.arraycopy(header, 0, wavData, 0, 44)
        System.arraycopy(pcmData, 0, wavData, 44, pcmData.size)
        return wavData
    }

    /**
     * Extracts acoustic features from PCM audio: RMS energy, zero-crossing rate,
     * high-frequency energy ratio, and onset rhythm for tempo inference.
     */
    fun analyzeAcousticVibe(pcmData: ByteArray): AudioVibeAnalysis {
        if (pcmData.isEmpty()) {
            return AudioVibeAnalysis(
                title = "Vibe: Clean Cristalino",
                detectedStyle = "Worship / Clean",
                gainVibe = "Limpo Cristalino",
                estimatedBpm = 110,
                suggestedAmp = "26CL2_JC120",
                suggestedCab = "61RJ120azz212",
                suggestedDrive = "MW-808",
                suggestedMod = "Chorus",
                suggestedDelay = "Analog",
                suggestedReverb = "Plate",
                noiseGateActive = false,
                details = "Timbre limpo e transparente com modulação suave e ambiência equilibrada.",
                rmsEnergy = 0.1f,
                highFreqRatio = 0.3f,
                base64Wav = ""
            )
        }

        val wav = pcmToWav(pcmData, SAMPLE_RATE)
        val base64Wav = Base64.encodeToString(wav, Base64.NO_WRAP)

        // Convert byte buffer to shorts
        val sampleCount = pcmData.size / 2
        val shorts = ShortArray(sampleCount)
        ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)

        var sumSquare = 0.0
        var zeroCrossings = 0
        var diffSum = 0.0
        var peakCount = 0
        var lastPeakSample = -SAMPLE_RATE

        for (i in 0 until sampleCount) {
            val sample = shorts[i]
            sumSquare += sample.toLong() * sample.toLong()

            if (i > 0) {
                if ((shorts[i - 1] >= 0 && sample < 0) || (shorts[i - 1] < 0 && sample >= 0)) {
                    zeroCrossings++
                }
                diffSum += abs(sample - shorts[i - 1])

                // Simple beat/onset energy threshold peak detector
                if (sample > 6000 && (i - lastPeakSample) > (SAMPLE_RATE / 4)) {
                    peakCount++
                    lastPeakSample = i
                }
            }
        }

        val rms = sqrt(sumSquare / sampleCount).toFloat()
        val zcr = zeroCrossings.toFloat() / sampleCount
        val highFreqRatio = if (rms > 0) (diffSum / sampleCount) / (rms + 1f) else 0f

        // Estimated BPM based on onsets or sensible fallback
        val durationSeconds = sampleCount.toFloat() / SAMPLE_RATE
        val rawBpm = if (durationSeconds > 0 && peakCount > 1) {
            ((peakCount.toFloat() / durationSeconds) * 60f).toInt()
        } else {
            115
        }
        val estimatedBpm = rawBpm.coerceIn(75, 160)

        // Determine Vibe & MK-20 Models based on real acoustic measurements
        return when {
            // High Energy + High Distortion (High RMS and High Zero Crossing Rate)
            rms > 5000f && zcr > 0.08f -> {
                AudioVibeAnalysis(
                    title = "Vibe: Heavy High-Gain",
                    detectedStyle = "Modern Metal / Rock Pesado",
                    gainVibe = "High Gain Agressivo",
                    estimatedBpm = estimatedBpm.coerceAtLeast(125),
                    suggestedAmp = "75DS_ENGL_120",
                    suggestedCab = "57MbTradS412B",
                    suggestedDrive = "2TS8",
                    suggestedMod = "Phaser",
                    suggestedDelay = "Clean",
                    suggestedReverb = "Room",
                    noiseGateActive = true,
                    details = "Graves apertados e corte agressivo de agudos identificado via microfone. Noise Gate ligado para bases percussivas.",
                    rmsEnergy = rms,
                    highFreqRatio = highFreqRatio.toFloat(),
                    base64Wav = base64Wav
                )
            }
            // Medium Energy + Crunchy Breakup (Classic Rock / Blues)
            rms > 2500f -> {
                AudioVibeAnalysis(
                    title = "Vibe: Classic Crunch & Blues",
                    detectedStyle = "Blues Rock / Vintage Breakup",
                    gainVibe = "Crunch Orgânico",
                    estimatedBpm = estimatedBpm,
                    suggestedAmp = "86OD_JCM800",
                    suggestedCab = "36Mar60B412A",
                    suggestedDrive = "MW-SD1",
                    suggestedMod = "Opto Tremolo",
                    suggestedDelay = "Dtype",
                    suggestedReverb = "Spring",
                    noiseGateActive = false,
                    details = "Saturação valvulada dinâmica com ataque médio identificada no áudio. Tube crunch clássico com reverb vintage.",
                    rmsEnergy = rms,
                    highFreqRatio = highFreqRatio.toFloat(),
                    base64Wav = base64Wav
                )
            }
            // Bright Single-Coil / Pop / Funk (Higher ZCR relative to volume)
            zcr > 0.07f -> {
                AudioVibeAnalysis(
                    title = "Vibe: Funk & Neo-Soul Clean",
                    detectedStyle = "Funk / Neo-Soul / Pop",
                    gainVibe = "Limpo Estalado com Ataque",
                    estimatedBpm = estimatedBpm,
                    suggestedAmp = "13CL1_FenDi",
                    suggestedCab = "11FdTR65R212B",
                    suggestedDrive = "MW-808",
                    suggestedMod = "Chorus",
                    suggestedDelay = "Analog",
                    suggestedReverb = "Plate",
                    noiseGateActive = false,
                    details = "Brilho de frequências agudas e ataque percussivo de palhetada identificado. Fender Deluxe cristalino com chorus refinado.",
                    rmsEnergy = rms,
                    highFreqRatio = highFreqRatio.toFloat(),
                    base64Wav = base64Wav
                )
            }
            // Ambient / Worship / Sweet Clean (Low RMS, smooth dynamics)
            else -> {
                AudioVibeAnalysis(
                    title = "Vibe: Worship Ambient & Shimmer",
                    detectedStyle = "Worship / Ambient / Balada",
                    gainVibe = "Limpo Cristalino com Ambiência",
                    estimatedBpm = estimatedBpm.coerceAtMost(115),
                    suggestedAmp = "26CL2_JC120",
                    suggestedCab = "20FdTwRev212A",
                    suggestedDrive = "MW-808",
                    suggestedMod = "Chorus Stereo",
                    suggestedDelay = "Analog",
                    suggestedReverb = "Shimmer stereo",
                    noiseGateActive = false,
                    details = "Ondas sonoras abertas e dinâmicas suaves identificadas no microfone. Roland JC-120 Clean com Shimmer Reverb e delay espacial.",
                    rmsEnergy = rms,
                    highFreqRatio = highFreqRatio.toFloat(),
                    base64Wav = base64Wav
                )
            }
        }
    }
}
