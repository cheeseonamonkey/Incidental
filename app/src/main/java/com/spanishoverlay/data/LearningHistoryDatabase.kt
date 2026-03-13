package com.spanishoverlay.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LearningEntry::class], version = 2, exportSchema = false)
abstract class LearningHistoryDatabase : RoomDatabase() {
    abstract fun historyDao(): LearningHistoryDao

    companion object {
        @Volatile private var INSTANCE: LearningHistoryDatabase? = null

        fun getInstance(context: Context): LearningHistoryDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LearningHistoryDatabase::class.java,
                    "learning_history.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build().also { INSTANCE = it }
            }
    }
}
