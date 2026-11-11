@file:Suppress("UnstableApiUsage")

package io.github.kimihrmn.clicolorhints

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.daemon.impl.InlayHintsPassFactoryInternal
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager

/**
 * The palette is caret-scoped, but inlay passes only re-run on document changes. When the caret enters or
 * leaves a string literal, restart the daemon for that file so the palette appears / disappears.
 *
 * The check here is PSI-only (no resolve) to stay cheap on the EDT; targeting happens in the inlay collector.
 */
class PaletteEditorFactoryListener : EditorFactoryListener {
    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        if (editor.project == null) return
        editor.caretModel.addCaretListener(PaletteCaretListener)
    }
}

object PaletteCaretListener : CaretListener {
    private val LAST_HOST: Key<TextRange> = Key.create("cliColorHints.lastStringHost")

    override fun caretPositionChanged(event: CaretEvent) {
        val editor = event.editor
        val project = editor.project ?: return
        if (project.isDisposed) return
        val docManager = PsiDocumentManager.getInstance(project)
        if (!docManager.isCommitted(editor.document)) return // typing restarts the daemon anyway
        val file = docManager.getPsiFile(editor.document) ?: return
        val lang = file.language.id
        if (lang != "JAVA" && lang != "kotlin") return

        val host = ReadAction.compute<TextRange?, RuntimeException> {
            ColorCodedTargeting.stringHostRange(file.findElementAt(editor.caretModel.offset))
        }
        if (host == editor.getUserData(LAST_HOST)) return
        editor.putUserData(LAST_HOST, host)
        refresh(editor)
    }

    private fun refresh(editor: Editor) {
        val project = editor.project ?: return
        val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return
        // The inlay pass is skipped when the file did not change; force it so the caret-scoped palette updates.
        InlayHintsPassFactoryInternal.forceHintsUpdateOnNextPass()
        DaemonCodeAnalyzer.getInstance(project).restart(file, "CLI color palette: caret entered/left a string")
    }
}
