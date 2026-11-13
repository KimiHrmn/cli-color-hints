package io.github.kimihrmn.clicolorhints

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorMathAndPreviewTest {

    @Test
    fun `blend and hex`() {
        assertEquals(0x808080, ColorMath.blend(0x000000, 0xFFFFFF, 0.5025))
        assertEquals("#300A24", ColorMath.hex(ColorCodes.TERMINAL_BACKGROUND))
    }

    @Test
    fun `preview html strips codes, escapes, colors placeholders`() {
        val html = PreviewRenderer.toHtml(
            ColorCodeParser.parse(listOf(StringPart.Text("&6a<b>&r&x"), StringPart.Dynamic("name"), StringPart.Text("\r\n"))),
        )
        assertFalse(html.contains("&6"))
        assertFalse(html.contains("\r"))
        assertTrue(html.contains("<span style=\"color:#FFC706;\">a&lt;b&gt;</span>"))
        assertTrue(html.contains("&amp;x‹name›"))
        assertTrue(html.contains("background-color:#300A24"))
        assertNotNull(html)
    }
}
