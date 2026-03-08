package com.spanishoverlay.pipeline

import com.spanishoverlay.data.ConfigRepository
import com.spanishoverlay.data.CountMode
import com.spanishoverlay.data.DictionaryEntry
import com.spanishoverlay.data.SpanishDictionary

class WordFilterPipeline(
    private val dictionary: SpanishDictionary,
    private val config: ConfigRepository
) {
    fun process(text: String): PipelineResult {
        val cfg = config.snapshot()
        val eligible = mutableListOf<Pair<Token, DictionaryEntry>>()

        for (token in Tokenizer.tokenize(text)) {
            val entry = dictionary.findAny(token.clean)
            if (FilterStage.Length.accepts(token, entry, cfg)
                && FilterStage.StopWord.accepts(token, entry, cfg)
                && FilterStage.Regex.accepts(token, entry, cfg)
                && FilterStage.Dictionary.accepts(token, entry, cfg)
                && FilterStage.Pos.accepts(token, entry, cfg)
                && FilterStage.Complexity.accepts(token, entry, cfg)
            ) eligible.add(token to entry!!)
        }

        val n = cfg.replaceEveryN.coerceAtLeast(1) // guard div-by-zero
        val selected = when (cfg.replaceCountMode) {
            CountMode.FRACTION -> eligible.filter { (t, _) ->
                kotlin.math.abs(t.clean.hashCode()) % n == 0
            }
            CountMode.FIXED -> eligible
                .shuffled(java.util.Random(text.hashCode().toLong()))
                .take(cfg.replaceFixedCount.coerceAtLeast(0))
        }

        return PipelineResult(
            replacements = selected.map { (_, e) -> Replacement(e.spanish, e.pos, e.complexity) },
            ttlMs = cfg.overlayTtlMs,
            alpha = cfg.overlayAlpha
        )
    }
}
