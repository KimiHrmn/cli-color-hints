@file:Suppress("UnstableApiUsage", "DEPRECATION")

package io.github.kimihrmn.clicolorhints

import com.intellij.codeInsight.hints.ChangeListener
import com.intellij.codeInsight.hints.FactoryInlayHintsCollector
import com.intellij.codeInsight.hints.ImmediateConfigurable
import com.intellij.codeInsight.hints.InlayGroup
import com.intellij.codeInsight.hints.InlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsProvider
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.NoSettings
import com.intellij.codeInsight.hints.SettingsKey
import com.intellij.codeInsight.hints.presentation.BasePresentation
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.MouseButton
import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLanguageInjectionHost
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Presentation-based inlays (the declarative API cannot paint colors):
 *  - a tiny color swatch before every `&X` token in a color-coded string;
 *  - while the caret is inside a color-coded string, a row of 17 clickable chips after it
 *    (click = insert the code at the caret, or wrap the selection).
 */
class ColorSwatchInlayProvider : InlayHintsProvider<NoSettings> {

    override val key: SettingsKey<NoSettings> = KEY
    override val name: String = "CLI color code swatches"
    override val group: InlayGroup = InlayGroup.OTHER_GROUP
    override val previewText: String? = null
    override val description: String = "Color swatches for console color codes and a clickable palette while editing a @ColorCoded string."

    override fun createSettings(): NoSettings = NoSettings()

    override fun isLanguageSupported(language: Language): Boolean = true

    override fun createConfigurable(settings: NoSettings): ImmediateConfigurable = object : ImmediateConfigurable {
        override fun createComponent(listener: ChangeListener): JComponent = JPanel()
    }

    override fun getCollectorFor(file: PsiFile, editor: Editor, settings: NoSettings, sink: InlayHintsSink): InlayHintsCollector =
        Collector(editor)

    private class Collector(editor: Editor) : FactoryInlayHintsCollector(editor) {
        private val seen = HashSet<TextRange>()
        private val caret = editor.caretModel.offset

        override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
            if (element !is PsiLanguageInjectionHost) return true
            val s = ColorCodedTargeting.find(element) ?: return true
            val range = s.range ?: return true
            if (!seen.add(range)) return true // Java concatenation: handle the whole expression once

            for (h in hintsFor(s, caret)) {
                when (h) {
                    is Hint.CodeSwatch ->
                        sink.addInlineElement(h.offset, false, factory.withTooltip(h.code.label, SwatchPresentation(editor, h.code, chip = false)), false)
                    is Hint.Palette ->
                        sink.addInlineElement(h.offset, true, palette(editor), false)
                }
            }
            return true
        }

        private fun palette(editor: Editor): InlayPresentation {
            val chips = ColorCodes.ALL.map { code ->
                val chip = factory.withTooltip(code.label, SwatchPresentation(editor, code, chip = true))
                factory.onClick(chip, MouseButton.Left) { _, _ ->
                    val project = editor.project ?: return@onClick
                    ColorCodeInsertion.apply(project, editor, code)
                }
            }
            return factory.seq(factory.textSpacePlaceholder(1, false), *chips.toTypedArray())
        }
    }

    sealed interface Hint {
        data class CodeSwatch(val offset: Int, val code: ColorCode) : Hint
        data class Palette(val offset: Int) : Hint
    }

    companion object {
        val KEY: SettingsKey<NoSettings> = SettingsKey("cli.color.hints.swatches")

        /** Pure decision of which inlays a string gets (testable). */
        fun hintsFor(s: ColorCodedString, caretOffset: Int): List<Hint> {
            val out = mutableListOf<Hint>()
            for (seg in ColorCodeParser.parse(s.rawParts())) {
                if (seg is Segment.Code && seg.offset != null) out += Hint.CodeSwatch(seg.offset, seg.code)
            }
            val end = s.range?.endOffset
            if (end != null && s.contentRangeAt(caretOffset) != null) out += Hint.Palette(end)
            return out
        }
    }
}

/** A filled color square; as a palette [chip] it is larger and labelled with the code character. */
class SwatchPresentation(
    private val editor: Editor,
    private val code: ColorCode,
    private val chip: Boolean,
) : BasePresentation() {

    private val lineHeight: Int get() = editor.lineHeight
    private val fontSize: Int get() = editor.colorsScheme.editorFontSize

    override val width: Int
        get() = if (chip) (fontSize * 1.4).toInt() + 2 else (fontSize * 0.7).toInt() + 3

    override val height: Int get() = lineHeight

    override fun paint(g: Graphics2D, attributes: TextAttributes) {
        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            val side = if (chip) (fontSize * 1.4).toInt() else (fontSize * 0.7).toInt()
            val x = 1
            val y = (lineHeight - side) / 2
            val fill = Color(code.rgb ?: ColorCodes.TERMINAL_BACKGROUND)
            g2.color = fill
            g2.fillRoundRect(x, y, side, side, 4, 4)
            g2.color = Color(ColorMath.blend(fill.rgb and 0xFFFFFF, editor.colorsScheme.defaultForeground.rgb and 0xFFFFFF, 0.5))
            g2.drawRoundRect(x, y, side - 1, side - 1, 4, 4)
            if (chip) {
                val fillRgb = fill.rgb and 0xFFFFFF
                val textRgb = if (ColorMath.contrastRatio(fillRgb, 0x000000) >= ColorMath.contrastRatio(fillRgb, 0xFFFFFF)) 0x000000 else 0xFFFFFF
                g2.color = Color(textRgb)
                g2.font = editor.colorsScheme.getFont(com.intellij.openapi.editor.colors.EditorFontType.PLAIN)
                    .deriveFont(Font.BOLD, (fontSize * 0.85f))
                val fm = g2.fontMetrics
                val label = code.char.toString()
                g2.drawString(label, x + (side - fm.stringWidth(label)) / 2, y + (side - fm.height) / 2 + fm.ascent)
            }
        } finally {
            g2.dispose()
        }
    }

    override fun toString(): String = if (chip) "[${code.token}]" else "■${code.token}"
}
