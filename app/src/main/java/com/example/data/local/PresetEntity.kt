package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Mk20Preset

@Entity(tableName = "saved_presets")
data class PresetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val song: String = "",
    val style: String = "",
    val bpm: Int = 120,
    val youtubeUrl: String = "",
    val details: String = "",
    val jsonContent: String,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toPreset(): Mk20Preset {
        return try {
            Mk20Preset.fromJsonString(jsonContent)
        } catch (e: Exception) {
            Mk20Preset(title = title, style = style, bpm = bpm, youtubeLink = youtubeUrl, details = details)
        }
    }

    companion object {
        fun fromPreset(preset: Mk20Preset, isFavorite: Boolean = false): PresetEntity {
            return PresetEntity(
                title = preset.title,
                song = preset.title,
                style = preset.style,
                bpm = preset.bpm,
                youtubeUrl = preset.youtubeLink,
                details = preset.details,
                jsonContent = preset.toJsonString(),
                isFavorite = isFavorite
            )
        }
    }
}
