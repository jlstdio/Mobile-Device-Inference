// In a new file: app/src/main/java/com/google/mediapipe/examples/llminference/AppDatabase.kt
package com.google.mediapipe.examples.llminference

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ChatMessageEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chat_database"
                )
                    .fallbackToDestructiveMigration() // Simple migration strategy for this example
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}