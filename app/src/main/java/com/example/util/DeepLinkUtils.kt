package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import com.example.data.model.Mk20Preset
import java.nio.charset.StandardCharsets

object DeepLinkUtils {
    private const val SCHEME = "mk20"
    private const val HOST = "preset"
    private const val WEB_HOST = "https://mk20tone.app/preset"

    /**
     * Creates a shareable deep link URL for an MK-20 preset.
     * Encodes the preset JSON into a base64 payload.
     */
    fun createShareUrl(preset: Mk20Preset, useWebScheme: Boolean = true): String {
        val json = preset.toJsonString(indent = 0)
        val base64Data = Base64.encodeToString(
            json.toByteArray(StandardCharsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP
        )
        return if (useWebScheme) {
            "$WEB_HOST?data=$base64Data"
        } else {
            "$SCHEME://$HOST?data=$base64Data"
        }
    }

    /**
     * Parses a Mk20Preset from a deep link Uri or web URL.
     */
    fun parsePresetFromUri(uri: Uri): Mk20Preset? {
        return try {
            val encodedData = uri.getQueryParameter("data")
                ?: uri.getQueryParameter("p")
                ?: uri.fragment

            if (!encodedData.isNullOrBlank()) {
                val decodedBytes = Base64.decode(encodedData, Base64.URL_SAFE or Base64.DEFAULT)
                val jsonStr = String(decodedBytes, StandardCharsets.UTF_8)
                Mk20Preset.fromJsonString(jsonStr)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Extracts YouTube video ID from various standard YouTube URL formats.
     */
    fun extractYouTubeId(url: String): String? {
        if (url.isBlank()) return null
        val patterns = listOf(
            Regex("(?:https?:\\/\\/)?(?:www\\.)?youtu\\.be\\/([a-zA-Z0-9_-]{11})"),
            Regex("(?:https?:\\/\\/)?(?:www\\.)?youtube\\.com\\/(?:embed\\/|v\\/|watch\\?v=|watch\\?.+&v=)([a-zA-Z0-9_-]{11})"),
            Regex("youtube\\.com\\/shorts\\/([a-zA-Z0-9_-]{11})")
        )
        for (pattern in patterns) {
            val match = pattern.find(url)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1]
            }
        }
        return null
    }

    /**
     * Shares a preset using Android system share sheet.
     */
    fun sharePreset(context: Context, preset: Mk20Preset) {
        val shareLink = createShareUrl(preset, useWebScheme = true)
        val deepLink = createShareUrl(preset, useWebScheme = false)
        val shareText = buildString {
            append("🎸 Preset M-VAVE MK-20: ${preset.title}\n")
            append("🎵 Estilo: ${preset.style} | BPM: ${preset.bpm}\n")
            if (preset.youtubeLink.isNotBlank()) {
                append("📺 YouTube: ${preset.youtubeLink}\n")
            }
            append("\n📲 Abrir no MK-20 Tone Generator:\n$shareLink\n")
            append("\nLink direto do app:\n$deepLink\n")
            append("\n--- JSON da Pedaleira ---\n")
            append(preset.toJsonString(indent = 2))
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, "Preset MK-20: ${preset.title}")
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Compartilhar Preset MK-20"))
    }
}
