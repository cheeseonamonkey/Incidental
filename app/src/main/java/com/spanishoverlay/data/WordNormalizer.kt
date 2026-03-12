package com.spanishoverlay.data

object WordNormalizer {
    fun normalize(raw: String): String = raw
        .lowercase()
        .replace('’', '\'')
        .trim()
        .trim { !it.isLetter() }
        .removeSuffix("'s")
        .removeSuffix("'")
        .replace(Regex("[-_]+"), "")

    fun candidates(raw: String, enabled: Boolean): List<String> {
        val base = normalize(raw)
        if (!enabled || base.isBlank()) return listOf(base).filter { it.isNotBlank() }
        return buildSet {
            add(base)
            if (base.endsWith("ies") && base.length > 3) add(base.dropLast(3) + "y")
            if (base.endsWith("es") && base.length > 2) add(base.dropLast(2))
            if (base.endsWith("s") && base.length > 1) add(base.dropLast(1))
            if (base.endsWith("ing") && base.length > 4) {
                add(base.dropLast(3))
                add(base.dropLast(3) + "e")
            }
            if (base.endsWith("ed") && base.length > 3) {
                add(base.dropLast(2))
                add(base.dropLast(2) + "e")
            }
        }.filter { it.isNotBlank() }
    }
}
