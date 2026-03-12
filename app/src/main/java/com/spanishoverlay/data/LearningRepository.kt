package com.spanishoverlay.data

import android.content.Context
import com.spanishoverlay.SpanishOverlayApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class LearningSignal(
    val key: String,
    val english: String,
    val spanish: String,
    val pos: PoS,
    val complexity: Int,
    val surface: String,
    val surfaced: Boolean
)

class LearningRepository private constructor(context: Context) {
    private val dao = LearningHistoryDatabase.getInstance(context).historyDao()
    private val scope = SpanishOverlayApp.INSTANCE.appScope
    private val _selection = MutableStateFlow<LearningSelection?>(null)
    val selectionFlow: StateFlow<LearningSelection?> = _selection.asStateFlow()

    val entriesFlow: StateFlow<Map<String, LearningEntry>> = dao.observeAll()
        .map { list -> list.associateBy(LearningEntry::key) }
        .stateIn(scope, SharingStarted.Eagerly, emptyMap())

    val recentFlow: StateFlow<List<LearningEntry>> = dao.observeRecent(12)
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val prioritizedFlow: StateFlow<List<LearningEntry>> = dao.observePrioritized(12)
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val ignoredFlow: StateFlow<List<LearningEntry>> = dao.observeIgnored(12)
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val statsFlow: StateFlow<LearningStats> = dao.observeAll()
        .map { list ->
            LearningStats(
                totalSeen = list.size,
                prioritized = list.count { it.priority },
                ignored = list.count { it.ignored },
                known = list.count { it.known }
            )
        }
        .stateIn(scope, SharingStarted.Eagerly, LearningStats())

    fun snapshot(): Map<String, LearningEntry> = entriesFlow.value

    suspend fun record(signals: List<LearningSignal>) {
        val now = System.currentTimeMillis()
        signals.groupBy { it.key }.values.forEach { grouped ->
            val last = grouped.last()
            val current = dao.find(last.key)
            val surfaced = grouped.any { it.surfaced }
            dao.upsert(
                LearningEntry(
                    key = last.key,
                    english = last.english,
                    spanish = last.spanish,
                    pos = last.pos.name,
                    complexity = last.complexity,
                    firstSeenAt = current?.firstSeenAt ?: now,
                    lastSeenAt = now,
                    lastSurfacedAt = if (surfaced) now else current?.lastSurfacedAt ?: 0L,
                    seenCount = (current?.seenCount ?: 0) + grouped.size,
                    surfacedCount = (current?.surfacedCount ?: 0) + grouped.count { it.surfaced },
                    priority = current?.priority ?: false,
                    ignored = current?.ignored ?: false,
                    known = current?.known ?: false,
                    lastSurfaceForm = last.surface
                )
            )
        }
    }

    suspend fun prioritize(selection: LearningSelection) = mutate(selection) { it.copy(priority = true, ignored = false, known = false) }
    suspend fun ignore(selection: LearningSelection) = mutate(selection) { it.copy(ignored = true, priority = false, known = false) }
    suspend fun markKnown(selection: LearningSelection) = mutate(selection) { it.copy(known = true, priority = false, ignored = false) }
    fun setSelection(selection: LearningSelection?) { _selection.value = selection }

    private suspend fun mutate(selection: LearningSelection, block: (LearningEntry) -> LearningEntry) {
        val now = System.currentTimeMillis()
        val current = dao.find(selection.key) ?: LearningEntry(
            key = selection.key,
            english = selection.english,
            spanish = selection.spanish,
            pos = selection.pos.name,
            complexity = 0,
            firstSeenAt = now,
            lastSeenAt = now,
            lastSurfacedAt = 0L,
            seenCount = 0,
            surfacedCount = 0,
            priority = false,
            ignored = false,
            known = false,
            lastSurfaceForm = selection.surface
        )
        dao.upsert(block(current.copy(lastSeenAt = now, lastSurfaceForm = selection.surface)))
    }

    companion object {
        @Volatile private var INSTANCE: LearningRepository? = null
        fun getInstance(ctx: Context) = INSTANCE ?: synchronized(this) {
            INSTANCE ?: LearningRepository(ctx.applicationContext).also { INSTANCE = it }
        }
    }
}
