package com.spanishoverlay.data

import android.content.SharedPreferences

data class OverlayConfig(
    // Frequency
    val replaceEveryN: Int = 5,
    val replaceCountMode: CountMode = CountMode.FRACTION,
    val replaceFixedCount: Int = 2,
    // Filters
    val minWordLength: Int = 4,
    val maxWordLength: Int = 30,
    val stopWordsEnabled: Boolean = true,
    val stopWords: Set<String> = DEFAULT_STOP_WORDS,
    val enabledPos: Set<PoS> = setOf(PoS.NOUN, PoS.VERB, PoS.ADJECTIVE),
    val complexityMin: Int = 0,
    val complexityMax: Int = 3,
    val phrasesEnabled: Boolean = true,
    // Timing
    val overlayTtlMs: Int = 4000,
    val fadeInMs: Int = 200,
    val fadeOutMs: Int = 400,
    val showDelayMinMs: Int = 0,
    val showDelayMaxMs: Int = 200,
    val debounceMs: Int = 300,
    // Appearance
    val overlayAlpha: Float = 0.85f,
    val fontScale: Float = 1.0f,
    val overlayTextColor: Int = 0xFFFFFFFF.toInt(),
    val overlayBgColor: Int = 0xCC141428.toInt(),
    val maxOverlays: Int = 20,
    val displayMode: DisplayMode = DisplayMode.ENGLISH_ARROW_SPANISH,
    val overlayPosition: OverlayPosition = OverlayPosition.ABOVE,
    val verticalOffsetDp: Int = 0,
    // Apps
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

        val TEXT_COLORS = listOf(
            0xFFFFFFFF.toInt(), 0xFFFFEB3B.toInt(), 0xFF00BCD4.toInt(),
            0xFF8BC34A.toInt(), 0xFFFF9800.toInt(), 0xFFE91E63.toInt(),
            0xFFBBDEFB.toInt(), 0xFFF48FB1.toInt()
        )
        val BG_COLORS = listOf(
            0xCC141428.toInt(), 0xCC000000.toInt(), 0xCC1B2838.toInt(),
            0xCC2E1B0E.toInt(), 0xCC0D2818.toInt(), 0xCC2A1B2E.toInt(),
            0x99000000.toInt(), 0x66000000.toInt()
        )

        val PRESET_BEGINNER = OverlayConfig(
            replaceEveryN = 8, complexityMin = 0, complexityMax = 1,
            enabledPos = setOf(PoS.NOUN), overlayTtlMs = 5000, phrasesEnabled = true
        )
        val PRESET_CASUAL = OverlayConfig(
            replaceEveryN = 5, complexityMin = 0, complexityMax = 2,
            enabledPos = setOf(PoS.NOUN, PoS.VERB, PoS.ADJECTIVE), overlayTtlMs = 4000, phrasesEnabled = true
        )
        val PRESET_LEARNER = OverlayConfig(
            replaceEveryN = 3, complexityMin = 0, complexityMax = 3,
            enabledPos = setOf(PoS.NOUN, PoS.VERB, PoS.ADJECTIVE, PoS.ADVERB), overlayTtlMs = 3500, phrasesEnabled = true
        )
        val PRESET_SCHOLAR = OverlayConfig(
            replaceEveryN = 2, complexityMin = 2, complexityMax = 3,
            enabledPos = PoS.entries.toSet(), overlayTtlMs = 3000, phrasesEnabled = true
        )

        fun fromPrefs(prefs: SharedPreferences) = OverlayConfig(
            replaceEveryN = prefs.getInt("replace_every_n", 5),
            replaceCountMode = pstr(prefs, "replace_count_mode", "FRACTION") { CountMode.valueOf(it) } ?: CountMode.FRACTION,
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
            phrasesEnabled = prefs.getBoolean("phrases_enabled", true),
            overlayTtlMs = prefs.getInt("overlay_ttl_ms", 4000),
            fadeInMs = prefs.getInt("fade_in_ms", 200),
            fadeOutMs = prefs.getInt("fade_out_ms", 400),
            showDelayMinMs = prefs.getInt("show_delay_min_ms", 0),
            showDelayMaxMs = prefs.getInt("show_delay_max_ms", 200),
            debounceMs = prefs.getInt("debounce_ms", 300),
            overlayAlpha = prefs.getFloat("overlay_alpha", 0.85f),
            fontScale = prefs.getFloat("font_scale", 1.0f),
            overlayTextColor = prefs.getInt("overlay_text_color", 0xFFFFFFFF.toInt()),
            overlayBgColor = prefs.getInt("overlay_bg_color", 0xCC141428.toInt()),
            maxOverlays = prefs.getInt("max_overlays", 20),
            displayMode = pstr(prefs, "display_mode", "ENGLISH_ARROW_SPANISH") { DisplayMode.valueOf(it) } ?: DisplayMode.ENGLISH_ARROW_SPANISH,
            overlayPosition = pstr(prefs, "overlay_position", "ABOVE") { OverlayPosition.valueOf(it) } ?: OverlayPosition.ABOVE,
            verticalOffsetDp = prefs.getInt("vertical_offset_dp", 0),
            includeHiddenViews = prefs.getBoolean("include_hidden_views", false),
            excludePackages = prefs.getStringSet("exclude_packages", emptySet()) ?: emptySet()
        )

        private fun <T> pstr(prefs: SharedPreferences, key: String, default: String, parse: (String) -> T): T? =
            runCatching { parse(prefs.getString(key, default) ?: default) }.getOrNull()
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
        putBoolean("phrases_enabled", phrasesEnabled)
        putInt("overlay_ttl_ms", overlayTtlMs)
        putInt("fade_in_ms", fadeInMs)
        putInt("fade_out_ms", fadeOutMs)
        putInt("show_delay_min_ms", showDelayMinMs)
        putInt("show_delay_max_ms", showDelayMaxMs)
        putInt("debounce_ms", debounceMs)
        putFloat("overlay_alpha", overlayAlpha)
        putFloat("font_scale", fontScale)
        putInt("overlay_text_color", overlayTextColor)
        putInt("overlay_bg_color", overlayBgColor)
        putInt("max_overlays", maxOverlays)
        putString("display_mode", displayMode.name)
        putString("overlay_position", overlayPosition.name)
        putInt("vertical_offset_dp", verticalOffsetDp)
        putBoolean("include_hidden_views", includeHiddenViews)
        putStringSet("exclude_packages", excludePackages)
    }.apply()
}
