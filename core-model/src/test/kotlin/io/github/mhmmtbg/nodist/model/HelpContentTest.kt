package io.github.mhmmtbg.nodist.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HelpContentTest {
    @Test
    fun emptyQueryReturnsEverythingInBothLanguages() {
        assertEquals(HelpContent.items(false).size, HelpContent.search("  ", false).size)
        assertEquals(HelpContent.items(true).size, HelpContent.items(false).size)
    }

    @Test
    fun searchIgnoresTurkishLettersAndCase() {
        assertTrue(HelpContent.search("KILAVUZ", false).any { it.title == "Kılavuzlar" })
        assertTrue(HelpContent.search("kilavuz", false).any { it.title == "Kılavuzlar" })
        assertTrue(HelpContent.search("bicak", false).any { it.title.startsWith("Bıçak") })
    }

    @Test
    fun searchFindsOtherLanguageAndShortcutKeys() {
        assertTrue(HelpContent.search("scissors", false).any { it.title.startsWith("Makas") })
        assertTrue(HelpContent.search("ctrl+j", true).any { it.isShortcut && it.keys == "Ctrl+J" })
        assertTrue(HelpContent.search("zzzzqq", false).isEmpty())
    }

    @Test
    fun allWordsMustMatchAndTitleHitsRankFirst() {
        val r = HelpContent.search("yol birleştir", false)
        assertTrue(r.isNotEmpty())
        assertTrue(r.first().title.contains("birleştir", ignoreCase = true))
        assertTrue(HelpContent.search("yol zzzzqq", false).isEmpty())
    }

    @Test
    fun everyToolShortcutIsDocumented() {
        val keys = HelpContent.items(false).filter { it.isShortcut }.map { it.keys }
        for (k in listOf("V", "A", "P", "N", "E", "C", "K", "M", "Q", "L", "Y", "U", "T", "I", "H", "F1")) assertTrue(k in keys, k)
    }
}
