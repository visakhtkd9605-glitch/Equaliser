package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EqualizerDao {
    @Query("SELECT * FROM genre_categories ORDER BY isBuiltIn DESC, name ASC")
    fun observeCategories(): Flow<List<GenreCategoryEntity>>

    @Query("SELECT COUNT(*) FROM genre_categories")
    suspend fun getCategoryCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: GenreCategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<GenreCategoryEntity>): List<Long>

    @Update
    suspend fun updateCategory(category: GenreCategoryEntity)

    @Query("DELETE FROM genre_categories WHERE id = :categoryId AND isBuiltIn = 0")
    suspend fun deleteCustomCategory(categoryId: Long)

    @Query("SELECT * FROM presets ORDER BY isBuiltIn DESC, updatedAt DESC")
    fun observePresets(): Flow<List<PresetEntity>>

    @Query("SELECT * FROM presets WHERE genreCategoryId = :categoryId ORDER BY name ASC")
    fun observePresetsByCategory(categoryId: Long): Flow<List<PresetEntity>>

    @Query("SELECT COUNT(*) FROM presets")
    suspend fun getPresetCount(): Int

    @Query("SELECT * FROM presets WHERE id = :id LIMIT 1")
    suspend fun getPresetById(id: Long): PresetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: PresetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresets(presets: List<PresetEntity>)

    @Update
    suspend fun updatePreset(preset: PresetEntity)

    @Query("UPDATE presets SET name = :newName, imageUri = :newImageUri, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updatePresetNameAndImage(id: Long, newName: String, newImageUri: String?, updatedAt: Long)

    @Query("UPDATE presets SET genreName = :newGenreName WHERE genreCategoryId = :categoryId")
    suspend fun syncGenreNameForCategory(categoryId: Long, newGenreName: String)

    @Query("DELETE FROM presets WHERE id = :presetId")
    suspend fun deletePresetById(presetId: Long)
}
