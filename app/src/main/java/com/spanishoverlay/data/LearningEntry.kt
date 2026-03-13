package com.spanishoverlay.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "learning_entries")
data class LearningEntry(
    @PrimaryKey val key: String,
    val english: String,
    val spanish: String,
    val pos: String,
    val complexity: Int,
    val firstSeenAt: Long,
    val lastSeenAt: Long,
    val lastSurfacedAt: Long,
    val seenCount: Int,
    val surfacedCount: Int,
    val priority: Boolean,
    val ignored: Boolean,
    val known: Boolean,
    val lastSurfaceForm: String = english,
    val nextReviewAt: Long = 0L
)

data class LearningStats(
    val totalSeen: Int = 0,
    val prioritized: Int = 0,
    val ignored: Int = 0,
    val known: Int = 0,
    val todaySeen: Int = 0,
    val streak: Int = 0
)

data class LearningSelection(
    val key: String,
    val english: String,
    val spanish: String,
    val surface: String,
    val pos: PoS
)
