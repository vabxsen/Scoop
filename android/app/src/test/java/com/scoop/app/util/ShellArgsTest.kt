package com.scoop.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ShellArgsTest {
    @Test
    fun splitsOnWhitespaceAndHonorsQuotes() {
        assertEquals(
            listOf("--output", "%(title)s.%(ext)s", "-x"),
            tokenizeShellArgs("  --output \"%(title)s.%(ext)s\"   -x "),
        )
    }

    @Test
    fun keepsEmptyQuotedValue() {
        assertEquals(listOf("--referer", "", "-x"), tokenizeShellArgs("--referer \"\" -x"))
        assertEquals(listOf("--referer", ""), tokenizeShellArgs("--referer ''"))
    }

    @Test
    fun joinsAdjacentQuotedAndBareText() {
        assertEquals(listOf("a b-c"), tokenizeShellArgs("'a b'-c"))
    }

    @Test
    fun blankInputHasNoTokens() {
        assertEquals(emptyList<String>(), tokenizeShellArgs("   "))
    }
}
