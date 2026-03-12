package com.spanishoverlay.pipeline

import com.spanishoverlay.data.PoS

data class Replacement(
    val key: String,
    val english: String,
    val spanish: String,
    val pos: PoS,
    val complexity: Int,
    val surface: String = english
)
