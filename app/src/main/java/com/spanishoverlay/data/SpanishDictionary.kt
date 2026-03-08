package com.spanishoverlay.data

import android.content.Context
import org.json.JSONArray

object SpanishDictionary {
    private val byEnglish = HashMap<String, DictionaryEntry>(2048)
    private val byInflection = HashMap<String, DictionaryEntry>(4096)
    val phraseIndex = LinkedHashMap<String, DictionaryEntry>() // longest-first for greedy match

    @Volatile private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            val raw = context.assets.open("dictionary.json").bufferedReader().readText()
            val arr = JSONArray(raw)
            val phrases = mutableListOf<Pair<String, DictionaryEntry>>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val en = obj.getString("en")
                val es = obj.getString("es")
                val pos = runCatching { PoS.valueOf(obj.getString("pos")) }.getOrDefault(PoS.OTHER)
                val c = obj.getInt("c")
                val altArr = obj.getJSONArray("alt")
                val alts = (0 until altArr.length()).map { altArr.getString(it) }
                val entry = DictionaryEntry(en, es, pos, c, alts)
                if (pos == PoS.PHRASE) {
                    phrases.add(en.lowercase() to entry)
                } else {
                    byEnglish[en.lowercase()] = entry
                    alts.forEach { byInflection[it.lowercase()] = entry }
                    byInflection[en.lowercase()] = entry
                }
            }
            // Sort phrases longest-first for greedy matching
            phrases.sortByDescending { it.first.length }
            phrases.forEach { (k, v) -> phraseIndex[k] = v }
            loaded = true
        }
    }

    fun findAny(word: String): DictionaryEntry? = byInflection[word.lowercase()]
        ?: byEnglish[word.lowercase()]

    fun size(): Int = byEnglish.size + byInflection.size / 2 + phraseIndex.size
}
