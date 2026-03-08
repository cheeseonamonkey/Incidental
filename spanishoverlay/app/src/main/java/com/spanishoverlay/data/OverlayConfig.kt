package com.spanishoverlay.data

import android.content.SharedPreferences

data class OverlayConfig(
    val replaceEveryN: Int = 5,
    val replaceCountMode: CountMode = CountMode.FRACTION,
    val replaceFixedCount: Int = 2,
    val minWordLength: Int = 4,
    val maxWordLength: Int = 30,
    val stopWordsEnabled: Boolean = true,
    val stopWords: Set<String> = DEFAULT_STOP_WORDS,
    val enabledPos: Set<PoS> = setOf(PoS.NOUN, PoS.VERB, PoS.ADJECTIVE),
    val complexityMin: Int = 0,
    val complexityMax: Int = 3,
    val overlayTtlMs: Int = 4000,
    val overlayAlpha: Float = 0.85f,
    val debounceMs: Int = 300,
    val includeHiddenViews: Boolean = false,
    val excludePackages: Set<String> = emptySet()
) {
    companion object {
        val DEFAULT = OverlayConfig()
        val DEFAULT_STOP_WORDS: Set<String> = setOf(
            "a","an","the","is","are","was","were","be","been","being",
            "have","has","had","do","does","did","will","would","could","should",
            "may","might","can","must","shall",
            "i","you","he","she","it","we","they","me","him","her","us","them",
            "my","your","his","its","our","their",
            "in","on","at","to","of","for","by","with","from","up","out",
            "as","if","or","and","but","not","so","yet","nor",
            "that","this","these","those","which","who","what",
            "when","where","how","than","then","there",
            "all","any","each","few","more","most","other","some","such",
            "no","only","same","very","just","also","even","still","here",
            "into","over","after","before","about","between"
        )

        fun fromPrefs(prefs: SharedPreferences) = OverlayConfig(
            replaceEveryN = prefs.getInt("replace_every_n", 5),
            replaceCountMode = runCatching {
                CountMode.valueOf(prefs.getString("replace_count_mode", "FRACTION")!!)
            }.getOrDefault(CountMode.FRACTION),
            replaceFixedCount = prefs.getInt("replace_fixed_count", 2),
            minWordLength = prefs.getInt("min_word_length", 4),
            maxWordLength = prefs.getInt("max_word_length", 30),
            stopWordsEnabled = prefs.getBoolean("stop_words_enabled", true),
            stopWords = prefs.getStringSet("stop_words", DEFAULT_STOP_WORDS) ?: DEFAULT_STOP_WORDS,
            enabledPos = (prefs.getStringSet("enabled_pos", setOf("NOUN","VERB","ADJECTIVE")) ?: emptySet())
                .mapNotNull { runCatching { PoS.valueOf(it) }.getOrNull() }.toSet()
                .ifEmpty { setOf(PoS.NOUN, PoS.VERB, PoS.ADJECTIVE) },
            complexityMin = prefs.getInt("complexity_min", 0),
            complexityMax = prefs.getInt("complexity_max", 3),
            overlayTtlMs = prefs.getInt("overlay_ttl_ms", 4000),
            overlayAlpha = prefs.getFloat("overlay_alpha", 0.85f),
            debounceMs = prefs.getInt("debounce_ms", 300),
            includeHiddenViews = prefs.getBoolean("include_hidden_views", false),
            excludePackages = prefs.getStringSet("exclude_packages", emptySet()) ?: emptySet()
        )
    }

    fun persist(prefs: SharedPreferences) = prefs.edit().apply {
        putInt("replace_every_n", replaceEveryN)
        putString("replace_count_mode", replaceCountMode.name)
        putInt("replace_fixed_count", replaceFixedCount)
        putInt("min_word_length", minWordLength)
        putInt("max_word_length", maxWordLength)
        putBoolean("stop_words_enabled", stopWordsEnabled)
        putStringSet("stop_words", stopWords)
        putStringSet("enabled_pos", enabledPos.map { it.name }.toSet())
        putInt("complexity_min", complexityMin)
        putInt("complexity_max", complexityMax)
        putInt("overlay_ttl_ms", overlayTtlMs)
        putFloat("overlay_alpha", overlayAlpha)
        putInt("debounce_ms", debounceMs)
        putBoolean("include_hidden_views", includeHiddenViews)
        putStringSet("exclude_packages", excludePackages)
    }.apply()
}
