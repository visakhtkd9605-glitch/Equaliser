package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [GenreCategoryEntity::class, PresetEntity::class],
    version = 1,
    exportSchema = false
)
abstract class EqualizerDatabase : RoomDatabase() {
    abstract fun equalizerDao(): EqualizerDao

    companion object {
        @Volatile
        private var INSTANCE: EqualizerDatabase? = null

        fun getInstance(context: Context): EqualizerDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    EqualizerDatabase::class.java,
                    "soniceq_database.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
