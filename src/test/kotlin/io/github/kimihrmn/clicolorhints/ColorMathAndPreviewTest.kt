package io.github.kimihrmn.clicolorhints

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorMathAndPreviewTest {

    @Test
    fun `contrast ratio extremes`() {
        assertEquals(21.0, ColorMath.contrastRatio(0x000000, 0xFFFFFF), 0.01)
        assertEquals(1.0, ColorMath.contrastRatio(0x777777, 0x777777), 0.0001)
    }

    @Test
    fun `chip only for low contrast and it is the contrasting one`() {
        val white = 0xFFFFFF
        // bright yellow on white editor -> needs dark chip
        assertEquals(ColorMath.DARK_CHIP, ColorMath.chipFor(0xE9AD0C, white))
        // black on dark editor -> needs light chip
        assertEquals(ColorMath.LIGHT_CHIP, ColorMath.chipFor(0x171421, 0x1E1F22))
        // dark blue on white is fine
        assertNull(ColorMath.chipFor(0x12488B, white))
    }

    @Test
    fun `every code is readable on its chip`() {
        for (bg in listOf(0xFFFFFF, 0x1E1F22, 0x2B2B2B)) {
            for (code in ColorCodes.ALL.filterNot { it.isReset }) {
                val chip = ColorMath.chipFor(code.rgb!!, bg) ?: continue
                assertTrue("${code.token} on chip", ColorMath.contrastRatio(code.rgb, chip) >= ColorMath.MIN_CONTRAST)
            }
        }
    }

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
        assertTrue(html.contains("<span style=\"color:#A2734C;\">a&lt;b&gt;</span>"))
        assertTrue(html.contains("&amp;x‹name›"))
        assertTrue(html.contains("background-color:#300A24"))
        assertNotNull(html)
    }
}
