package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Query("SELECT * FROM saved_presets ORDER BY createdAt DESC")
    fun getAllPresets(): Flow<List<PresetEntity>>

    @Query("SELECT * FROM saved_presets WHERE id = :id LIMIT 1")
    suspend fun getPresetById(id: Int): PresetEntity?

    @Query("SELECT * FROM saved_presets WHERE title LIKE '%' || :query || '%' OR style LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchPresets(query: String): Flow<List<PresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: PresetEntity): Long

    @Update
    suspend fun updatePreset(preset: PresetEntity)

    @Delete
    suspend fun deletePreset(preset: PresetEntity)

    @Query("DELETE FROM saved_presets WHERE id = :id")
    suspend fun deletePresetById(id: Int)

    @Query("SELECT COUNT(*) FROM saved_presets")
    suspend fun getCount(): Int
}
