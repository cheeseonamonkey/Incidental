package com.spanishoverlay.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LearningHistoryDao {
    @Query("SELECT * FROM learning_entries")
    fun observeAll(): Flow<List<LearningEntry>>

    @Query("SELECT * FROM learning_entries ORDER BY lastSeenAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<LearningEntry>>

    @Query("SELECT * FROM learning_entries WHERE priority = 1 ORDER BY lastSeenAt DESC LIMIT :limit")
    fun observePrioritized(limit: Int): Flow<List<LearningEntry>>

    @Query("SELECT * FROM learning_entries WHERE ignored = 1 ORDER BY lastSeenAt DESC LIMIT :limit")
    fun observeIgnored(limit: Int): Flow<List<LearningEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: LearningEntry)

    @Query("SELECT * FROM learning_entries WHERE `key` = :key LIMIT 1")
    suspend fun find(key: String): LearningEntry?

    @Query("SELECT COUNT(*) FROM learning_entries WHERE firstSeenAt >= :startOfDay")
    fun observeTodaySeen(startOfDay: Long): Flow<Int>
}
