package com.spanishoverlay.pipeline

import com.spanishoverlay.data.DictionaryEntry
import com.spanishoverlay.data.OverlayConfig
import java.util.regex.Pattern

sealed interface FilterStage {
    fun accepts(token: Token, entry: DictionaryEntry?, cfg: OverlayConfig): Boolean

    object Length : FilterStage {
        override fun accepts(token: Token, entry: DictionaryEntry?, cfg: OverlayConfig) =
            token.clean.length in cfg.minWordLength..cfg.maxWordLength
    }
    object StopWord : FilterStage {
        override fun accepts(token: Token, entry: DictionaryEntry?, cfg: OverlayConfig) =
            !cfg.stopWordsEnabled || token.clean.lowercase() !in cfg.stopWords
    }
    object Regex : FilterStage {
        private val P = Pattern.compile("""^\d+$|^@|^#|^https?://|^[A-Z]{2,}$""")
        override fun accepts(token: Token, entry: DictionaryEntry?, cfg: OverlayConfig) =
            !P.matcher(token.clean).matches()
    }
    object Dictionary : FilterStage {
        override fun accepts(token: Token, entry: DictionaryEntry?, cfg: OverlayConfig) = entry != null
    }
    object Pos : FilterStage {
        override fun accepts(token: Token, entry: DictionaryEntry?, cfg: OverlayConfig) =
            entry?.pos in cfg.enabledPos
    }
    object Complexity : FilterStage {
        override fun accepts(token: Token, entry: DictionaryEntry?, cfg: OverlayConfig) =
            entry != null && entry.complexity in cfg.complexityMin..cfg.complexityMax
    }
}
