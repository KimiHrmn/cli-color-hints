package io.github.kimihrmn.clicolorhints

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile

/**
 * Shared "insert code at caret, or wrap the selection" behavior for completion and swatch clicks.
 *
 * Selection inside one string literal's content -> `&X` + selection + `&r`; otherwise insert `&X` at the caret.
 * `&r` never wraps (it would produce `&r…&r`); with a selection it is inserted at the selection start.
 */
object ColorCodeInsertion {

    /** Text that was selected when `&` was typed; completion wraps it with `&r` after the chosen code. */
    val PENDING_WRAP: Key<RangeMarker> = Key.create("cliColorHints.pendingWrap")

    /**
     * Finds the color-coded string at the caret (committing the document first).
     * With a selection, the selection start is used as anchor.
     */
    fun stringAtCaret(project: Project, editor: Editor): Pair<PsiFile, ColorCodedString>? {
        val docManager = PsiDocumentManager.getInstance(project)
        docManager.commitDocument(editor.document)
        val file = docManager.getPsiFile(editor.document) ?: return null
        val sel = editor.selectionModel
        val offset = if (sel.hasSelection()) sel.selectionStart else editor.caretModel.offset
        val s = ColorCodedTargeting.find(file.findElementAt(offset))
            ?: ColorCodedTargeting.find(file.findElementAt((offset - 1).coerceAtLeast(0)))
            ?: return null
        if (s.contentRangeAt(offset) == null) return null
        return file to s
    }

    /** The selection, if it lies completely inside one literal content range of [s]. */
    fun selectionInside(editor: Editor, s: ColorCodedString): TextRange? {
        val sel = editor.selectionModel
        if (!sel.hasSelection()) return null
        val r = TextRange(sel.selectionStart, sel.selectionEnd)
        return if (s.contentRanges().any { it.contains(r) }) r else null
    }

    /** Swatch click: insert [code] at caret or wrap the selection. Must be called on EDT. */
    fun apply(project: Project, editor: Editor, code: ColorCode) {
        val (_, s) = stringAtCaret(project, editor) ?: return
        val wrap = selectionInside(editor, s)
        WriteCommandAction.runWriteCommandAction(project, "Insert Color Code ${code.token}", null, {
            applyInWriteAction(editor, code, wrap)
        })
    }

    /** Document change part of [apply]; caller holds the write lock / command. */
    fun applyInWriteAction(editor: Editor, code: ColorCode, wrap: TextRange?) {
        val doc = editor.document
        if (wrap != null && !code.isReset) {
            doc.insertString(wrap.endOffset, ColorCodes.RESET.token)
            doc.insertString(wrap.startOffset, code.token)
            editor.selectionModel.removeSelection()
            editor.caretModel.moveToOffset(wrap.endOffset + code.token.length + ColorCodes.RESET.token.length)
        } else {
            val at = wrap?.startOffset ?: editor.caretModel.offset
            editor.selectionModel.removeSelection()
            doc.insertString(at, code.token)
            editor.caretModel.moveToOffset(at + code.token.length)
        }
    }
}
