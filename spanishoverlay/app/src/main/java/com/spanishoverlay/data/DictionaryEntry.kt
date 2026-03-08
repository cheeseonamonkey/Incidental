package com.spanishoverlay.data

data class DictionaryEntry(
    val english: String,
    val spanish: String,
    val pos: PoS,
    val complexity: Int,
    val altForms: List<String> = emptyList()
)
