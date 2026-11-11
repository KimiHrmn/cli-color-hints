package io.github.kimihrmn.clicolorhints

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorCodesTest {

    @Test
    fun `mapping matches agreed SGR table`() {
        val expected = mapOf(
            '0' to 30, '1' to 34, '2' to 32, '3' to 36, '4' to 31, '5' to 35, '6' to 33, '7' to 37,
            '8' to 90, '9' to 94, 'a' to 92, 'b' to 96, 'c' to 91, 'd' to 95, 'e' to 93, 'f' to 97, 'r' to 0,
        )
        assertEquals(expected, ColorCodes.ALL.associate { it.char to it.sgr })
    }

    @Test
    fun `seventeen codes, only reset has no rgb`() {
        assertEquals(17, ColorCodes.ALL.size)
        assertEquals(listOf(ColorCodes.RESET), ColorCodes.ALL.filter { it.isReset })
        assertEquals('r', ColorCodes.RESET.char)
    }

    @Test
    fun `labels are plain names with ANSI hint`() {
        assertEquals("&6 Yellow (dark) · ANSI 33", ColorCodes.byChar('6')!!.label)
        assertEquals("&e Yellow (bright) · ANSI 93", ColorCodes.byChar('e')!!.label)
        assertEquals("&r Reset · ANSI 0", ColorCodes.RESET.label)
    }

    @Test
    fun `lookup is case sensitive and rejects unknown codes`() {
        assertNull(ColorCodes.byChar('A'))
        assertNull(ColorCodes.byChar('x'))
        assertNull(ColorCodes.byChar('l'))
        assertNull(ColorCodes.byToken("&"))
        assertNull(ColorCodes.byToken("6"))
        assertEquals('c', ColorCodes.byToken("&c")!!.char)
    }

    @Test
    fun `tokens are unique`() {
        assertTrue(ColorCodes.ALL.map { it.token }.toSet().size == ColorCodes.ALL.size)
    }
}
