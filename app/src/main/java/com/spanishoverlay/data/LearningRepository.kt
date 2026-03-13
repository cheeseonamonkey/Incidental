package com.spanishoverlay.data

import android.content.Context
import com.spanishoverlay.SpanishOverlayApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

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

    val statsFlow: StateFlow<LearningStats> = combine(
        entriesFlow,
        dao.observeTodaySeen(startOfToday())
    ) { map, todaySeen ->
        val list = map.values
        LearningStats(
            totalSeen = list.size,
            prioritized = list.count { it.priority },
            ignored = list.count { it.ignored },
            known = list.count { it.known },
            todaySeen = todaySeen,
            streak = computeStreak(list)
        )
    }.stateIn(scope, SharingStarted.Eagerly, LearningStats())

    fun snapshot(): Map<String, LearningEntry> = entriesFlow.value

    suspend fun record(signals: List<LearningSignal>) {
        val now = System.currentTimeMillis()
        signals.groupBy { it.key }.values.forEach { grouped ->
            val last = grouped.last()
            val current = dao.find(last.key)
            val surfaced = grouped.any { it.surfaced }
            val newSurfacedCount = (current?.surfacedCount ?: 0) + grouped.count { it.surfaced }
            val nextReviewAt = if (surfaced) computeNextReview(newSurfacedCount, now) else current?.nextReviewAt ?: 0L
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
                    surfacedCount = newSurfacedCount,
                    priority = current?.priority ?: false,
                    ignored = current?.ignored ?: false,
                    known = current?.known ?: false,
                    lastSurfaceForm = last.surface,
                    nextReviewAt = nextReviewAt
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
        // SRS doubling intervals: 1h, 1d, 3d, 7d, 21d, then stable at 21d
        private val SRS_INTERVALS_MS = longArrayOf(
            3_600_000L, 86_400_000L, 259_200_000L, 604_800_000L, 1_814_400_000L
        )

        fun computeNextReview(surfacedCount: Int, now: Long): Long {
            val interval = SRS_INTERVALS_MS.getOrElse(surfacedCount - 1) { SRS_INTERVALS_MS.last() }
            return now + interval
        }

        fun startOfToday(): Long {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        fun computeStreak(entries: Collection<LearningEntry>): Int {
            if (entries.isEmpty()) return 0
            val cal = Calendar.getInstance()
            var streak = 0
            var dayStart = startOfToday()
            // Check today first; if no activity today, streak may still count from yesterday
            val dayMs = 86_400_000L
            var checkDay = dayStart
            while (true) {
                val dayEnd = checkDay + dayMs
                if (entries.any { it.firstSeenAt >= checkDay && it.firstSeenAt < dayEnd }) {
                    streak++
                    checkDay -= dayMs
                } else {
                    // Allow checking yesterday if today has no activity yet
                    if (checkDay == dayStart) {
                        checkDay -= dayMs
                        val yesterday = checkDay
                        if (entries.any { it.firstSeenAt >= yesterday && it.firstSeenAt < yesterday + dayMs }) {
                            streak++
                            checkDay -= dayMs
                            continue
                        }
                    }
                    break
                }
            }
            return streak
        }

        @Volatile private var INSTANCE: LearningRepository? = null
        fun getInstance(ctx: Context) = INSTANCE ?: synchronized(this) {
            INSTANCE ?: LearningRepository(ctx.applicationContext).also { INSTANCE = it }
        }
    }
}
