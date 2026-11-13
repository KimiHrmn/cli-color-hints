package io.github.kimihrmn.clicolorhints

/**
 * One console color code, e.g. `&6`.
 *
 * @property char the character after `&`
 * @property sgr the 4-bit ANSI SGR parameter the lib emits (`ESC[<sgr>m`)
 * @property rgb preview color as 0xRRGGBB (Ubuntu terminal palette, Wikipedia "ANSI escape code" Ubuntu column); `null` for reset
 * @property displayName plain-language name, no Minecraft names
 */
data class ColorCode(
    val char: Char,
    val sgr: Int,
    val rgb: Int?,
    val displayName: String,
) {
    /** The two-character token as written in source, e.g. `&6`. */
    val token: String get() = "&$char"

    val isReset: Boolean get() = rgb == null

    /** e.g. `&6 Yellow (dark) · ANSI 33` */
    val label: String get() = "$token $displayName · ANSI $sgr"
}

/**
 * The single source of truth for code -> SGR -> preview color -> name.
 *
 * VERIFY against your library (SGR numbers) and a real Ubuntu terminal (RGB values).
 */
object ColorCodes {
    const val PREFIX = '&'

    /** Ubuntu GNOME Terminal default profile background ("aubergine"). Best guess, verify. */
    const val TERMINAL_BACKGROUND: Int = 0x300A24

    /** Ubuntu GNOME Terminal default profile foreground. */
    const val TERMINAL_FOREGROUND: Int = 0xFFFFFF

    val ALL: List<ColorCode> = listOf(
        ColorCode('0', 30, 0x010101, "Black"),
        ColorCode('1', 34, 0x006FB8, "Blue (dark)"),
        ColorCode('2', 32, 0x39B54A, "Green (dark)"),
        ColorCode('3', 36, 0x2CB5E9, "Cyan (dark)"),
        ColorCode('4', 31, 0xDE382B, "Red (dark)"),
        ColorCode('5', 35, 0x762671, "Magenta (dark)"),
        ColorCode('6', 33, 0xFFC706, "Yellow (dark)"),
        ColorCode('7', 37, 0xCCCCCC, "Gray (light)"),
        ColorCode('8', 90, 0x808080, "Gray (dark)"),
        ColorCode('9', 94, 0x0000FF, "Blue (bright)"),
        ColorCode('a', 92, 0x00FF00, "Green (bright)"),
        ColorCode('b', 96, 0x00FFFF, "Cyan (bright)"),
        ColorCode('c', 91, 0xFF0000, "Red (bright)"),
        ColorCode('d', 95, 0xFF00FF, "Magenta (bright)"),
        ColorCode('e', 93, 0xFFFF00, "Yellow (bright)"),
        ColorCode('f', 97, 0xFFFFFF, "White"),
        ColorCode('r', 0, null, "Reset"),
    )

    val RESET: ColorCode = ALL.last()

    private val byChar: Map<Char, ColorCode> = ALL.associateBy { it.char }

    /** Codes are case-sensitive: only lowercase `a-f` and `r` are recognised. */
    fun byChar(c: Char): ColorCode? = byChar[c]

    fun byToken(token: String): ColorCode? =
        if (token.length == 2 && token[0] == PREFIX) byChar(token[1]) else null
}
