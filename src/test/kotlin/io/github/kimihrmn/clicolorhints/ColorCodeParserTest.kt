package io.github.kimihrmn.clicolorhints

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorCodeParserTest {

    private fun c(ch: Char) = ColorCodes.byChar(ch)!!

    @Test
    fun `parses codes, text and reset`() {
        val segs = ColorCodeParser.parse("&6Hello&c!&r\r\n")
        assertEquals(
            listOf(
                Segment.Code(c('6'), null),
                Segment.Text("Hello", null, c('6')),
                Segment.Code(c('c'), null),
                Segment.Text("!", null, c('c')),
                Segment.Code(ColorCodes.RESET, null),
                Segment.Text("\r\n", null, null),
            ),
            segs,
        )
    }

    @Test
    fun `text before first code has default color`() {
        val segs = ColorCodeParser.parse("plain &aGreen")
        assertEquals(Segment.Text("plain ", null, null), segs[0])
        assertEquals(Segment.Text("Green", null, c('a')), segs[2])
    }

    @Test
    fun `unknown codes and lone ampersand are plain text`() {
        val segs = ColorCodeParser.parse("&xA &lB & &A &")
        assertEquals(listOf(Segment.Text("&xA &lB & &A &", null, null)), segs)
    }

    @Test
    fun `double ampersand is not an escape`() {
        // && is unknown: first & is plain text, second & starts a code.
        val segs = ColorCodeParser.parse("&&6x")
        assertEquals(
            listOf(Segment.Text("&", null, null), Segment.Code(c('6'), null), Segment.Text("x", null, c('6'))),
            segs,
        )
    }

    @Test
    fun `adjacent codes - last one wins`() {
        val segs = ColorCodeParser.parse("&1&2x")
        assertEquals(Segment.Text("x", null, c('2')), segs.last())
        assertEquals(3, segs.size)
    }

    @Test
    fun `offsets are absolute when part offset is given`() {
        val segs = ColorCodeParser.parse(listOf(StringPart.Text("ab&6cd", 100)))
        assertEquals(
            listOf(
                Segment.Text("ab", 100, null),
                Segment.Code(c('6'), 102),
                Segment.Text("cd", 104, c('6')),
            ),
            segs,
        )
    }

    @Test
    fun `color carries across parts and into placeholders`() {
        val segs = ColorCodeParser.parse(
            listOf(StringPart.Text("&6Hi "), StringPart.Dynamic("name"), StringPart.Text("!&r"), StringPart.Dynamic("x")),
        )
        assertEquals(
            listOf(
                Segment.Code(c('6'), null),
                Segment.Text("Hi ", null, c('6')),
                Segment.Placeholder("name", c('6')),
                Segment.Text("!", null, c('6')),
                Segment.Code(ColorCodes.RESET, null),
                Segment.Placeholder("x", null),
            ),
            segs,
        )
    }

    @Test
    fun `codes do not span parts`() {
        val segs = ColorCodeParser.parse(listOf(StringPart.Text("a&"), StringPart.Text("6b")))
        assertTrue(segs.none { it is Segment.Code })
    }

    @Test
    fun `strip removes codes and renders placeholders`() {
        val segs = ColorCodeParser.parse(listOf(StringPart.Text("&6Hi "), StringPart.Dynamic("name"), StringPart.Text("&r!")))
        assertEquals("Hi ‹name›!", ColorCodeParser.strip(segs))
    }

    @Test
    fun `empty input gives no segments`() {
        assertTrue(ColorCodeParser.parse("").isEmpty())
        assertNull(ColorCodeParser.parse("&r").first().color)
    }
}
