package com.spanishoverlay.pipeline

import com.spanishoverlay.data.ConfigRepository
import com.spanishoverlay.data.CountMode
import com.spanishoverlay.data.DictionaryEntry
import com.spanishoverlay.data.PoS
import com.spanishoverlay.data.SpanishDictionary

class WordFilterPipeline(private val config: ConfigRepository) {

    fun process(text: String): PipelineResult {
        val cfg = config.snapshot()
        val replacements = mutableListOf<Replacement>()
        val excludeRanges = mutableListOf<IntRange>()

        // Phase 1: phrase matching (greedy, longest-first)
        if (cfg.phrasesEnabled && PoS.PHRASE in cfg.enabledPos) {
            val lower = text.lowercase()
            for ((phrase, entry) in SpanishDictionary.phraseIndex) {
                var start = 0
                while (true) {
                    val idx = lower.indexOf(phrase, start)
                    if (idx < 0) break
                    val range = idx until idx + phrase.length
                    // Skip if overlaps existing match
                    if (excludeRanges.none { it.first <= range.last && range.first <= it.last }) {
                        excludeRanges.add(range)
                        replacements.add(Replacement(entry.english, entry.spanish, PoS.PHRASE, entry.complexity))
                    }
                    start = idx + 1
                }
            }
        }

        // Phase 2: word-level pipeline
        val eligible = mutableListOf<Pair<Token, DictionaryEntry>>()
        for (token in Tokenizer.tokenize(text, excludeRanges)) {
            val entry = SpanishDictionary.findAny(token.clean)
            if (FilterStage.Length.accepts(token, entry, cfg)
                && FilterStage.StopWord.accepts(token, entry, cfg)
                && FilterStage.Regex.accepts(token, entry, cfg)
                && FilterStage.Dictionary.accepts(token, entry, cfg)
                && FilterStage.Pos.accepts(token, entry, cfg)
                && FilterStage.Complexity.accepts(token, entry, cfg)
            ) eligible.add(token to entry!!)
        }

        val n = cfg.replaceEveryN.coerceAtLeast(1)
        val selected = when (cfg.replaceCountMode) {
            CountMode.FRACTION -> eligible.filter { (t, _) ->
                kotlin.math.abs(t.clean.hashCode()) % n == 0
            }
            CountMode.FIXED -> eligible
                .shuffled(java.util.Random(text.hashCode().toLong()))
                .take(cfg.replaceFixedCount.coerceAtLeast(0))
        }

        replacements.addAll(selected.map { (t, e) -> Replacement(t.clean, e.spanish, e.pos, e.complexity) })

        return PipelineResult(
            replacements = replacements,
            ttlMs = cfg.overlayTtlMs,
            alpha = cfg.overlayAlpha
        )
    }
}
