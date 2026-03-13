package com.spanishoverlay.pipeline

import com.spanishoverlay.data.ConfigRepository
import com.spanishoverlay.data.CountMode
import com.spanishoverlay.data.DictionaryEntry
import com.spanishoverlay.data.LearningEntry
import com.spanishoverlay.data.LearningRepository
import com.spanishoverlay.data.LearningSignal
import com.spanishoverlay.data.PoS
import com.spanishoverlay.data.SpanishDictionary

private data class Candidate(val token: Token, val entry: DictionaryEntry, val score: Float)

class WordFilterPipeline(
    private val config: ConfigRepository,
    private val learning: LearningRepository
) {

    suspend fun process(text: String): PipelineResult {
        val cfg = config.snapshot()
        val replacements = mutableListOf<Replacement>()
        val signals = mutableListOf<LearningSignal>()
        val excludeRanges = mutableListOf<IntRange>()
        val history = learning.snapshot()

        // Phase 1: phrase matching (greedy, longest-first)
        if (cfg.phrasesEnabled) {
            val lower = text.lowercase()
            for ((phrase, entry) in SpanishDictionary.phraseEntries()) {
                var start = 0
                while (true) {
                    val idx = lower.indexOf(phrase, start)
                    if (idx < 0) break
                    val range = idx until idx + phrase.length
                    // Skip if overlaps existing match
                    if (isPhraseBoundary(lower, idx, phrase.length)
                        && excludeRanges.none { it.first <= range.last && range.first <= it.last }) {
                        excludeRanges.add(range)
                        replacements.add(Replacement(entry.key, entry.english, entry.spanish, PoS.PHRASE, entry.complexity, phrase))
                        signals.add(entry.toSignal(phrase, surfaced = true))
                    }
                    start = idx + 1
                }
            }
        }

        // Phase 2: word-level pipeline
        val eligible = mutableListOf<Candidate>()
        for (token in Tokenizer.tokenize(text, excludeRanges)) {
            val entry = SpanishDictionary.findAny(token.clean, cfg.normalizationEnabled)
            val state = entry?.let { history[it.key] }
            if (FilterStage.Length.accepts(token, entry, cfg)
                && FilterStage.StopWord.accepts(token, entry, cfg)
                && FilterStage.Regex.accepts(token, entry, cfg)
                && FilterStage.Dictionary.accepts(token, entry, cfg)
                && FilterStage.Pos.accepts(token, entry, cfg)
                && FilterStage.Complexity.accepts(token, entry, cfg)
                && state?.ignored != true
                && state?.known != true
            ) eligible.add(Candidate(token, entry!!, score(token, entry, state, cfg)))
        }

        val n = cfg.replaceEveryN.coerceAtLeast(1)
        val selected = when (cfg.replaceCountMode) {
            CountMode.FRACTION -> eligible.filter {
                kotlin.math.abs(it.token.clean.hashCode()) % n == 0
            }
            CountMode.FIXED -> eligible
                .sortedByDescending(Candidate::score)
                .take(cfg.replaceFixedCount.coerceAtLeast(0))
            CountMode.ALL -> eligible.sortedByDescending(Candidate::score)
        }

        val selectedSet = selected.toHashSet()
        signals.addAll(eligible.map { it.entry.toSignal(it.token.clean, surfaced = it in selectedSet) })
        replacements.addAll(selected.map { Replacement(it.entry.key, it.entry.english, it.entry.spanish, it.entry.pos, it.entry.complexity, it.token.clean) })
        learning.record(signals)

        return PipelineResult(
            replacements = replacements,
            ttlMs = cfg.overlayTtlMs,
            alpha = cfg.overlayAlpha
        )
    }

    private fun score(token: Token, entry: DictionaryEntry, state: LearningEntry?, cfg: com.spanishoverlay.data.OverlayConfig): Float {
        val priority = if (state?.priority == true) 4f else 0f
        val knownPenalty = if (state?.known == true) -100f else 0f
        val ignoredPenalty = if (state?.ignored == true) -100f else 0f
        val novelty = if (state == null) 3f else 0.5f / (state.surfacedCount + 1)
        val phraseBoost = if (entry.pos == PoS.PHRASE) 1.5f else 0f
        val now = System.currentTimeMillis()
        val srsBoost = if (state != null && state.nextReviewAt > 0L && state.nextReviewAt <= now) 2f else 0f
        val repeatBoost = if (state != null && state.lastSurfacedAt > 0L) cfg.repeatRecentWeight * recentBoost(state.lastSurfacedAt) else 0f
        val complexityBoost = entry.complexity * 0.15f
        val stableJitter = (kotlin.math.abs(token.clean.hashCode() xor entry.key.hashCode()) % 100) / 1000f
        return priority + novelty + phraseBoost + srsBoost + repeatBoost + complexityBoost + stableJitter + knownPenalty + ignoredPenalty
    }

    private fun recentBoost(lastSurfacedAt: Long): Float {
        val minutes = ((System.currentTimeMillis() - lastSurfacedAt).coerceAtLeast(0L) / 60000f)
        return when {
            minutes < 2f -> 0.4f
            minutes < 10f -> 1f
            minutes < 60f -> 0.6f
            else -> 0.15f
        }
    }

    private fun isPhraseBoundary(text: String, start: Int, length: Int): Boolean {
        val before = text.getOrNull(start - 1)
        val after = text.getOrNull(start + length)
        return !before.isWordChar() && !after.isWordChar()
    }

    private fun Char?.isWordChar() = this?.isLetterOrDigit() == true

    private fun DictionaryEntry.toSignal(surface: String, surfaced: Boolean) =
        LearningSignal(key, english, spanish, pos, complexity, surface, surfaced)
}
