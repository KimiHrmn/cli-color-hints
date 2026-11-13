package io.github.kimihrmn.clicolorhints

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.FoldRegion
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.util.PsiTreeUtil

/**
 * Hides the `&X` tokens of color-coded strings (an empty fold placeholder) and shows them while the caret is
 * inside the string. The builder only decides the initial state of a region; [CodeFoldingCaretListener] toggles
 * the regions when the caret enters or leaves a string.
 */
class ColorCodeFoldingBuilder : FoldingBuilderEx() {

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        if (DumbService.isDumb(root.project)) return FoldingDescriptor.EMPTY_ARRAY // targeting needs resolve
        val out = mutableListOf<FoldingDescriptor>()
        for (host in PsiTreeUtil.collectElements(root) { it is PsiLanguageInjectionHost }) {
            val s = ColorCodedTargeting.find(host) ?: continue
            for (seg in ColorCodeParser.parse(s.rawParts())) {
                val at = (seg as? Segment.Code)?.offset ?: continue
                val range = TextRange(at, at + seg.code.token.length)
                if (host.textRange.contains(range)) out += FoldingDescriptor(host.node, range) // concatenation: each literal its own
            }
        }
        return out.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String = PLACEHOLDER

    /** Collapsed unless a caret is inside the string the region belongs to. */
    override fun isCollapsedByDefault(node: ASTNode): Boolean {
        val file = node.psi.containingFile ?: return true
        val document = PsiDocumentManager.getInstance(file.project).getDocument(file) ?: return true
        val range = node.textRange
        return EditorFactory.getInstance().getEditors(document, file.project)
            .none { CodeFoldingCaretListener.isInside(range, it.caretModel.offset) }
    }

    companion object {
        /** Empty: the code disappears completely. Also how [CodeFoldingCaretListener] recognizes its regions. */
        const val PLACEHOLDER: String = ""
    }
}

/** Expands the code tokens of the string the caret enters and collapses those of the one it leaves. */
class FoldingEditorFactoryListener : EditorFactoryListener {
    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        if (editor.project == null) return
        editor.caretModel.addCaretListener(CodeFoldingCaretListener)
    }
}

object CodeFoldingCaretListener : CaretListener {
    private val LAST_HOST: Key<TextRange> = Key.create("cliColorHints.lastStringHost")

    /** Caret strictly between the quotes (or the text block delimiters) of a string at [range]. */
    fun isInside(range: TextRange, offset: Int): Boolean = offset > range.startOffset && offset < range.endOffset

    override fun caretPositionChanged(event: CaretEvent) {
        val editor = event.editor
        val project = editor.project ?: return
        if (project.isDisposed) return
        val docManager = PsiDocumentManager.getInstance(project)
        if (!docManager.isCommitted(editor.document)) return // typing restarts the folding pass anyway
        val file = docManager.getPsiFile(editor.document) ?: return
        val lang = file.language.id
        if (lang != "JAVA" && lang != "kotlin") return

        // PSI only, no resolve: cheap on the EDT. Non-targeted strings have no regions to toggle.
        val offset = editor.caretModel.offset
        val host = ReadAction.computeBlocking<TextRange?, RuntimeException> {
            ColorCodedTargeting.stringHostRange(file.findElementAt(offset))?.takeIf { isInside(it, offset) }
        }
        val last = editor.getUserData(LAST_HOST)
        if (host == last) return
        editor.putUserData(LAST_HOST, host)
        toggle(editor, collapseIn = last, expandIn = host)
    }

    private fun toggle(editor: Editor, collapseIn: TextRange?, expandIn: TextRange?) {
        val model = editor.foldingModel
        model.runBatchFoldingOperation {
            for (region in model.allFoldRegions) {
                if (!isCodeRegion(editor, region)) continue
                val r = TextRange(region.startOffset, region.endOffset)
                if (expandIn != null && expandIn.contains(r)) region.isExpanded = true
                else if (collapseIn != null && collapseIn.contains(r)) region.isExpanded = false
            }
        }
    }

    private fun isCodeRegion(editor: Editor, region: FoldRegion): Boolean =
        region.isValid && region.placeholderText == ColorCodeFoldingBuilder.PLACEHOLDER &&
            ColorCodes.byToken(editor.document.getText(TextRange(region.startOffset, region.endOffset))) != null
}
