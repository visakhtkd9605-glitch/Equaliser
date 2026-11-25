package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "genre_categories")
data class GenreCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val accentColorHex: Long = 0xFF22D3EE,
    val isBuiltIn: Boolean = false
)
