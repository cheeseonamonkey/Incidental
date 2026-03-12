package com.spanishoverlay.data

import android.content.Context
import org.json.JSONArray

object SpanishDictionary {
    @Volatile private var byEnglish: Map<String, DictionaryEntry> = emptyMap()
    @Volatile private var byInflection: Map<String, DictionaryEntry> = emptyMap()
    @Volatile private var phraseIndex: Map<String, DictionaryEntry> = emptyMap() // longest-first for greedy match

    @Volatile private var loaded = false

    fun ensureLoaded(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            val raw = context.assets.open("dictionary.json").bufferedReader().readText()
            val arr = JSONArray(raw)
            val english = HashMap<String, DictionaryEntry>(2048)
            val inflections = HashMap<String, DictionaryEntry>(4096)
            val phrases = mutableListOf<Pair<String, DictionaryEntry>>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val en = obj.getString("en")
                val es = obj.getString("es")
                val pos = runCatching { PoS.valueOf(obj.getString("pos")) }.getOrDefault(PoS.OTHER)
                val c = obj.getInt("c")
                val altArr = obj.getJSONArray("alt")
                val alts = (0 until altArr.length()).map { altArr.getString(it) }
                val key = WordNormalizer.normalize(en)
                val entry = DictionaryEntry(key, en, es, pos, c, alts)
                if (pos == PoS.PHRASE) {
                    phrases.add(key to entry)
                } else {
                    english[key] = entry
                    (listOf(en) + alts).flatMap { WordNormalizer.candidates(it, true) }.forEach { inflections[it] = entry }
                }
            }
            // Sort phrases longest-first for greedy matching
            phrases.sortByDescending { it.first.length }
            byEnglish = english
            byInflection = inflections
            phraseIndex = LinkedHashMap<String, DictionaryEntry>(phrases.size).apply {
                phrases.forEach { (k, v) -> put(k, v) }
            }
            loaded = true
        }
    }

    fun load(context: Context) = ensureLoaded(context)

    fun phraseEntries(): Map<String, DictionaryEntry> = phraseIndex

    fun findAny(word: String, normalize: Boolean = true): DictionaryEntry? =
        WordNormalizer.candidates(word, normalize).firstNotNullOfOrNull { byInflection[it] ?: byEnglish[it] }

    fun size(): Int = byEnglish.size + byInflection.size / 2 + phraseIndex.size
}
