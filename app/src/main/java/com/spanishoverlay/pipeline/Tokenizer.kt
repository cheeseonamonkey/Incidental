package com.spanishoverlay.pipeline

object Tokenizer {
    fun tokenize(text: String, excludeRanges: List<IntRange> = emptyList()): List<Token> {
        if (excludeRanges.isEmpty()) {
            return text.split(Regex("""\s+""")).mapNotNull { raw ->
                val clean = raw.trim().trimStart { !it.isLetter() }.trimEnd { !it.isLetter() }
                if (clean.isBlank()) null else Token(raw, clean)
            }
        }
        // Build text with excluded ranges replaced by spaces
        val sb = StringBuilder(text)
        for (r in excludeRanges) {
            for (i in r) if (i < sb.length) sb[i] = ' '
        }
        return sb.toString().split(Regex("""\s+""")).mapNotNull { raw ->
            val clean = raw.trim().trimStart { !it.isLetter() }.trimEnd { !it.isLetter() }
            if (clean.isBlank()) null else Token(raw, clean)
        }
    }
}
