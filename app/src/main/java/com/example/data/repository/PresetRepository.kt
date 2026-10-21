package com.example.data.repository

import com.example.data.local.PresetDao
import com.example.data.local.PresetEntity
import com.example.data.model.Mk20Preset
import kotlinx.coroutines.flow.Flow

class PresetRepository(private val presetDao: PresetDao) {
    val allPresets: Flow<List<PresetEntity>> = presetDao.getAllPresets()

    fun searchPresets(query: String): Flow<List<PresetEntity>> {
        return presetDao.searchPresets(query)
    }

    suspend fun savePreset(preset: Mk20Preset): Long {
        return presetDao.insertPreset(PresetEntity.fromPreset(preset))
    }

    suspend fun deletePreset(presetEntity: PresetEntity) {
        presetDao.deletePreset(presetEntity)
    }

    suspend fun deletePresetById(id: Int) {
        presetDao.deletePresetById(id)
    }

    suspend fun updateFavorite(entity: PresetEntity, isFav: Boolean) {
        presetDao.updatePreset(entity.copy(isFavorite = isFav))
    }

    suspend fun seedFactoryPresetsIfNeeded() {
        if (presetDao.getCount() == 0) {
            Mk20Preset.FACTORY_PRESETS.forEach { preset ->
                presetDao.insertPreset(PresetEntity.fromPreset(preset))
            }
        }
    }
}
