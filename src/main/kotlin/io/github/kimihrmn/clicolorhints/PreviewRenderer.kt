package io.github.kimihrmn.clicolorhints

/** Renders parsed segments as an HTML terminal-like preview (codes stripped, Ubuntu palette). */
object PreviewRenderer {

    fun toHtml(segments: List<Segment>): String = buildString {
        append("<pre style=\"")
        append("background-color:").append(ColorMath.hex(ColorCodes.TERMINAL_BACKGROUND)).append(';')
        append("color:").append(ColorMath.hex(ColorCodes.TERMINAL_FOREGROUND)).append(';')
        append("padding:6px;font-family:monospace;\">")
        for (s in segments) {
            val text = when (s) {
                is Segment.Code -> continue
                is Segment.Text -> s.text
                is Segment.Placeholder -> ColorCodeParser.placeholder(s.name)
            }
            val visible = escape(text.replace("\r", ""))
            if (visible.isEmpty()) continue
            val rgb = s.color?.rgb
            if (rgb == null) {
                append(visible)
            } else {
                append("<span style=\"color:").append(ColorMath.hex(rgb)).append(";\">").append(visible).append("</span>")
            }
        }
        append("</pre>")
    }

    private fun escape(s: String): String = buildString(s.length) {
        for (c in s) when (c) {
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '&' -> append("&amp;")
            '"' -> append("&quot;")
            else -> append(c)
        }
    }
}
