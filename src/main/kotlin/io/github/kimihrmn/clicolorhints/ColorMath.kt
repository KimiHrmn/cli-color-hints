package io.github.kimihrmn.clicolorhints

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** Pure color helpers (no AWT) so they are unit-testable. Colors are 0xRRGGBB ints. */
object ColorMath {
    /** Below this WCAG contrast ratio, text gets a contrasting background chip in the editor. */
    const val MIN_CONTRAST: Double = 2.5

    const val DARK_CHIP: Int = 0x2B2B2B
    const val LIGHT_CHIP: Int = 0xF2F2F2

    fun relativeLuminance(rgb: Int): Double {
        fun channel(v: Int): Double {
            val c = v / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(rgb shr 16 and 0xFF) +
            0.7152 * channel(rgb shr 8 and 0xFF) +
            0.0722 * channel(rgb and 0xFF)
    }

    fun contrastRatio(a: Int, b: Int): Double {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    /** The background chip to put behind [fg] when it is hard to read on [editorBg], or `null` if readable. */
    fun chipFor(fg: Int, editorBg: Int): Int? {
        if (contrastRatio(fg, editorBg) >= MIN_CONTRAST) return null
        return if (contrastRatio(fg, DARK_CHIP) >= contrastRatio(fg, LIGHT_CHIP)) DARK_CHIP else LIGHT_CHIP
    }

    /** Linear blend; `t = 0` gives [a], `t = 1` gives [b]. */
    fun blend(a: Int, b: Int, t: Double): Int {
        fun ch(shift: Int): Int {
            val x = a shr shift and 0xFF
            val y = b shr shift and 0xFF
            return (x + (y - x) * t).toInt().coerceIn(0, 255)
        }
        return (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    fun hex(rgb: Int): String = "#%06X".format(rgb and 0xFFFFFF)
}
