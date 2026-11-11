package io.github.kimihrmn.clicolorhints

/** One piece of a (possibly concatenated / templated) color-coded string. */
sealed interface StringPart {
    /**
     * Literal text.
     * @property offset absolute document offset of `text[0]`, or `null` when ranges are not needed (preview).
     */
    data class Text(val text: String, val offset: Int? = null) : StringPart

    /** A dynamic part (`$name`, `+ name`), rendered as `‹name›`. */
    data class Dynamic(val name: String) : StringPart
}

/** Parser output. [color] is the color in effect for the segment (`null` = terminal default). */
sealed interface Segment {
    val color: ColorCode?

    /** A recognised `&X` token. [offset] is the absolute offset of `&` (if known). */
    data class Code(val code: ColorCode, val offset: Int?) : Segment {
        override val color: ColorCode? get() = if (code.isReset) null else code
    }

    /** Plain text. [offset] is the absolute offset of `text[0]` (if known). */
    data class Text(val text: String, val offset: Int?, override val color: ColorCode?) : Segment

    data class Placeholder(val name: String, override val color: ColorCode?) : Segment
}

object ColorCodeParser {

    fun parse(text: String): List<Segment> = parse(listOf(StringPart.Text(text)))

    /**
     * Splits parts into segments. The active color carries across parts.
     * Codes never span two parts. Unknown codes (`&x`, `&l`, lone `&`) are plain text.
     */
    fun parse(parts: List<StringPart>): List<Segment> {
        val out = mutableListOf<Segment>()
        var color: ColorCode? = null
        for (part in parts) {
            when (part) {
                is StringPart.Dynamic -> out += Segment.Placeholder(part.name, color)
                is StringPart.Text -> {
                    val s = part.text
                    var textStart = 0
                    var i = 0
                    while (i < s.length) {
                        val code = if (s[i] == ColorCodes.PREFIX && i + 1 < s.length) ColorCodes.byChar(s[i + 1]) else null
                        if (code == null) {
                            i++
                            continue
                        }
                        if (i > textStart) out += Segment.Text(s.substring(textStart, i), part.offset?.plus(textStart), color)
                        out += Segment.Code(code, part.offset?.plus(i))
                        color = if (code.isReset) null else code
                        i += 2
                        textStart = i
                    }
                    if (textStart < s.length) out += Segment.Text(s.substring(textStart), part.offset?.plus(textStart), color)
                }
            }
        }
        return out
    }

    /** The visible text with codes stripped and placeholders as `‹name›`. */
    fun strip(segments: List<Segment>): String = buildString {
        for (s in segments) when (s) {
            is Segment.Code -> Unit
            is Segment.Text -> append(s.text)
            is Segment.Placeholder -> append(placeholder(s.name))
        }
    }

    fun placeholder(name: String): String = "‹$name›"
}
