package io.github.kimihrmn.clicolorhints

/** Pure color helpers (no AWT) so they are unit-testable. Colors are 0xRRGGBB ints. */
object ColorMath {
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
