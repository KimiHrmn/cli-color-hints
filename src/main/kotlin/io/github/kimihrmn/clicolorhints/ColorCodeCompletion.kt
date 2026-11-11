package io.github.kimihrmn.clicolorhints

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.CompletionConfidence
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.PlainPrefixMatcher
import com.intellij.codeInsight.completion.CompletionSorter
import com.intellij.codeInsight.lookup.LookupElementWeigher
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.ThreeState
import com.intellij.util.ui.ColorIcon
import java.awt.Color

/** Completion of `&X` codes inside color-coded strings. */
class ColorCodeCompletionContributor : CompletionContributor() {

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        val prefixStart = codePrefixStart(parameters.editor.document.charsSequence, parameters.offset) ?: return
        val s = ColorCodedTargeting.find(parameters.originalFile.findElementAt(prefixStart)) ?: return
        val range = s.contentRangeAt(parameters.offset) ?: return
        if (prefixStart < range.startOffset) return

        val prefix = parameters.editor.document.charsSequence.subSequence(prefixStart, parameters.offset).toString()
        val rs = result.withPrefixMatcher(PlainPrefixMatcher(prefix)).withRelevanceSorter(TABLE_ORDER)
        ColorCodes.ALL.forEach { rs.addElement(lookupElement(it)) }
        rs.stopHere()
    }

    companion object {
        private const val MAX_WORD = 16

        /**
         * Keep items in table order (0-9, a-f, r) instead of alphabetical.
         * Lazy: `emptySorter()` fetches CompletionService, and services must not be touched in `<clinit>`.
         */
        private val TABLE_ORDER: CompletionSorter by lazy {
            CompletionSorter.emptySorter().weigh(
                object : LookupElementWeigher("cliColorHints.tableOrder") {
                    override fun weigh(element: LookupElement): Comparable<*> {
                        val i = ColorCodes.ALL.indexOf(element.`object`)
                        return if (i < 0) Int.MAX_VALUE else i
                    }
                },
            )
        }

        /** Offset of the `&` that starts the code prefix before [offset] (`&`, `&6`, `&yel`), or `null`. */
        fun codePrefixStart(text: CharSequence, offset: Int): Int? {
            var i = offset - 1
            while (i >= 0 && offset - i <= MAX_WORD && text[i].isLetterOrDigit()) i--
            return if (i >= 0 && text[i] == ColorCodes.PREFIX) i else null
        }

        fun lookupElement(code: ColorCode): LookupElement {
            val word = code.displayName.substringBefore(' ').lowercase()
            return LookupElementBuilder.create(code, code.token)
                .withLookupString("&$word")
                .withPresentableText(code.token)
                .withTailText("  ${code.displayName}", true)
                .withTypeText("ANSI ${code.sgr}")
                .withIcon(SwatchIcons.icon(code))
                .withInsertHandler(WrapInsertHandler)
        }
    }

    /** After the code is inserted, closes a pending selection wrap with `&r`. */
    object WrapInsertHandler : InsertHandler<LookupElement> {
        override fun handleInsert(context: com.intellij.codeInsight.completion.InsertionContext, item: LookupElement) {
            val editor = context.editor
            val marker = editor.getUserData(ColorCodeInsertion.PENDING_WRAP) ?: return
            editor.putUserData(ColorCodeInsertion.PENDING_WRAP, null)
            val code = item.`object` as? ColorCode
            if (code != null && !code.isReset && marker.isValid && marker.startOffset == context.tailOffset) {
                val end = marker.endOffset
                context.document.insertString(end, ColorCodes.RESET.token)
                editor.caretModel.moveToOffset(end + ColorCodes.RESET.token.length)
            }
            marker.dispose()
        }
    }
}

/** Auto-popup on `&`, and wrap-on-selection when `&` is typed over a selection. */
class ColorCodeTypedHandler : TypedHandlerDelegate() {

    override fun beforeSelectionRemoved(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
        if (c != ColorCodes.PREFIX) return Result.CONTINUE
        val sel = editor.selectionModel
        if (!sel.hasSelection()) return Result.CONTINUE
        PsiDocumentManager.getInstance(project).commitDocument(editor.document)
        val s = ColorCodedTargeting.find(file.findElementAt(sel.selectionStart)) ?: return Result.CONTINUE
        val wrap = ColorCodeInsertion.selectionInside(editor, s) ?: return Result.CONTINUE

        WriteAction.run<RuntimeException> {
            editor.getUserData(ColorCodeInsertion.PENDING_WRAP)?.dispose()
            editor.document.insertString(wrap.startOffset, ColorCodes.PREFIX.toString())
            val marker = editor.document.createRangeMarker(wrap.startOffset + 1, wrap.endOffset + 1)
            editor.putUserData(ColorCodeInsertion.PENDING_WRAP, marker)
            sel.removeSelection()
            editor.caretModel.moveToOffset(wrap.startOffset + 1)
        }
        AutoPopupController.getInstance(project).scheduleAutoPopup(editor)
        return Result.STOP
    }

    override fun checkAutoPopup(charTyped: Char, project: Project, editor: Editor, file: PsiFile): Result {
        if (charTyped != ColorCodes.PREFIX) return Result.CONTINUE
        val offset = editor.caretModel.offset
        val s = ColorCodedTargeting.find(file.findElementAt(offset))
            ?: ColorCodedTargeting.find(file.findElementAt((offset - 1).coerceAtLeast(0)))
            ?: return Result.CONTINUE
        if (s.contentRangeAt(offset) == null) return Result.CONTINUE
        AutoPopupController.getInstance(project).scheduleAutoPopup(editor)
        return Result.STOP
    }
}

/** Platform/Java/Kotlin skip auto-popup inside string literals; allow it right after `&` in color-coded strings. */
class ColorCodeCompletionConfidence : CompletionConfidence() {
    override fun shouldSkipAutopopup(editor: Editor, contextElement: PsiElement, psiFile: PsiFile, offset: Int): ThreeState {
        val start = ColorCodeCompletionContributor.codePrefixStart(editor.document.charsSequence, offset) ?: return ThreeState.UNSURE
        val s = ColorCodedTargeting.find(psiFile.findElementAt(start)) ?: return ThreeState.UNSURE
        return if (s.contentRangeAt(offset) != null) ThreeState.NO else ThreeState.UNSURE
    }
}

/** Small color icons for completion items. */
object SwatchIcons {
    fun icon(code: ColorCode): javax.swing.Icon {
        val rgb = code.rgb
        return if (rgb == null) ColorIcon(12, Color(ColorCodes.TERMINAL_FOREGROUND), true)
        else ColorIcon(12, Color(rgb), true)
    }
}
