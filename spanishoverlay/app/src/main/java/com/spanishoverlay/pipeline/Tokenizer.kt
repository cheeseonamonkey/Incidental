package com.spanishoverlay.pipeline

object Tokenizer {
    fun tokenize(text: String): List<Token> =
        text.split(Regex("""\s+""")).mapNotNull { raw ->
            val clean = raw.trim().trimStart { !it.isLetter() }.trimEnd { !it.isLetter() }
            if (clean.isBlank()) null else Token(raw, clean)
        }
}
