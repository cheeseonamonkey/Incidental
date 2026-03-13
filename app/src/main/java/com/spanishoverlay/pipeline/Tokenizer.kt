package com.spanishoverlay.pipeline

object Tokenizer {
    private val WS = Regex("""\s+""")

    fun tokenize(text: String, excludeRanges: List<IntRange> = emptyList()): List<Token> {
        val src = if (excludeRanges.isEmpty()) text else {
            val sb = StringBuilder(text)
            for (r in excludeRanges) for (i in r) if (i < sb.length) sb[i] = ' '
            sb.toString()
        }
        return src.split(WS).mapNotNull { raw ->
            val clean = raw.trimStart { !it.isLetter() }.trimEnd { !it.isLetter() }
            if (clean.isBlank()) null else Token(raw, clean)
        }
    }
}
