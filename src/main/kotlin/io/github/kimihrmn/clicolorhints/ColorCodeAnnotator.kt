package io.github.kimihrmn.clicolorhints

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import java.awt.Color

/**
 * Colors text after a code with the code's preview color (plus a contrasting background chip when it would be
 * hard to read on the editor background) and dims the `&X` tokens themselves.
 */
class ColorCodeAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is PsiLanguageInjectionHost) return
        val s = ColorCodedTargeting.find(element) ?: return
        val hostRange = element.textRange
        val scheme = EditorColorsManager.getInstance().globalScheme
        val editorBg = scheme.defaultBackground.rgb and 0xFFFFFF
        val editorFg = scheme.defaultForeground.rgb and 0xFFFFFF

        for (span in spans(s, editorBg, editorFg)) {
            if (!hostRange.contains(span.range)) continue // Java concat: each literal annotates its own part
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(span.range)
                .enforcedTextAttributes(span.attributes())
                .create()
        }
    }

    /** One highlighted range: [fg]/[bg] as 0xRRGGBB, [dim] marks a code token. */
    data class Span(val range: TextRange, val fg: Int, val bg: Int?, val dim: Boolean) {
        fun attributes(): TextAttributes = TextAttributes().apply {
            foregroundColor = Color(fg)
            if (bg != null) backgroundColor = Color(bg)
        }
    }

    companion object {
        /** How far code tokens are blended from the editor foreground towards the background. */
        private const val DIM = 0.55

        /** Pure computation of the highlight spans (testable without highlighting infrastructure). */
        fun spans(s: ColorCodedString, editorBg: Int, editorFg: Int): List<Span> {
            val dimFg = ColorMath.blend(editorFg, editorBg, DIM)
            val out = mutableListOf<Span>()
            for (seg in ColorCodeParser.parse(s.rawParts())) {
                when (seg) {
                    is Segment.Code -> {
                        val at = seg.offset ?: continue
                        out += Span(TextRange(at, at + seg.code.token.length), dimFg, null, dim = true)
                    }
                    is Segment.Text -> {
                        val at = seg.offset ?: continue
                        val rgb = seg.color?.rgb ?: continue
                        if (seg.text.isEmpty()) continue
                        out += Span(TextRange(at, at + seg.text.length), rgb, ColorMath.chipFor(rgb, editorBg), dim = false)
                    }
                    is Segment.Placeholder -> Unit
                }
            }
            return out
        }
    }
}
