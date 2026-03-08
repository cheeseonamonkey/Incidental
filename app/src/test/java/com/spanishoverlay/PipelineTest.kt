package com.spanishoverlay

import com.spanishoverlay.data.*
import com.spanishoverlay.pipeline.*
import org.junit.Assert.*
import org.junit.Test

class PipelineTest {

    @Test fun `tokenizer splits whitespace and trims punctuation`() {
        val tokens = Tokenizer.tokenize("Hello, world! Test.")
        assertEquals(listOf("Hello", "world", "Test"), tokens.map { it.clean })
    }

    @Test fun `tokenizer skips blank tokens`() {
        val tokens = Tokenizer.tokenize("  a   b  ")
        assertEquals(2, tokens.size)
    }

    @Test fun `stop word filter rejects common words`() {
        val cfg = OverlayConfig.DEFAULT
        val token = Token("the", "the")
        assertFalse(FilterStage.StopWord.accepts(token, null, cfg))
    }

    @Test fun `stop word filter passes non-stop words`() {
        val cfg = OverlayConfig.DEFAULT
        val token = Token("house", "house")
        assertTrue(FilterStage.StopWord.accepts(token, null, cfg))
    }

    @Test fun `regex filter rejects numbers`() {
        val cfg = OverlayConfig.DEFAULT
        assertFalse(FilterStage.Regex.accepts(Token("123", "123"), null, cfg))
    }

    @Test fun `regex filter rejects URLs`() {
        val cfg = OverlayConfig.DEFAULT
        assertFalse(FilterStage.Regex.accepts(Token("https://x.com", "https://x.com"), null, cfg))
    }

    @Test fun `regex filter rejects all-caps abbreviations`() {
        val cfg = OverlayConfig.DEFAULT
        assertFalse(FilterStage.Regex.accepts(Token("NASA", "NASA"), null, cfg))
    }

    @Test fun `length filter respects min and max`() {
        val cfg = OverlayConfig(minWordLength = 4, maxWordLength = 10)
        assertFalse(FilterStage.Length.accepts(Token("hi", "hi"), null, cfg))
        assertTrue(FilterStage.Length.accepts(Token("house", "house"), null, cfg))
        assertFalse(FilterStage.Length.accepts(Token("extraordinary", "extraordinary"), null, cfg))
    }

    @Test fun `complexity filter respects range`() {
        val cfg = OverlayConfig(complexityMin = 0, complexityMax = 1)
        val entry0 = DictionaryEntry("house", "casa", PoS.NOUN, 0)
        val entry3 = DictionaryEntry("ephemeral", "efímero", PoS.ADJECTIVE, 3)
        assertTrue(FilterStage.Complexity.accepts(Token("house", "house"), entry0, cfg))
        assertFalse(FilterStage.Complexity.accepts(Token("ephemeral", "ephemeral"), entry3, cfg))
    }

    @Test fun `pos filter respects enabled set`() {
        val cfg = OverlayConfig(enabledPos = setOf(PoS.NOUN))
        val noun = DictionaryEntry("house", "casa", PoS.NOUN, 0)
        val verb = DictionaryEntry("run", "correr", PoS.VERB, 0)
        assertTrue(FilterStage.Pos.accepts(Token("house", "house"), noun, cfg))
        assertFalse(FilterStage.Pos.accepts(Token("run", "run"), verb, cfg))
    }
}
