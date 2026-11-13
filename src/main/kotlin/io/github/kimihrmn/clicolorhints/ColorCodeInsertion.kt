package io.github.kimihrmn.clicolorhints

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange

/**
 * Wrap-the-selection support for completion: a selection inside one string literal's content becomes
 * `&X` + selection + `&r` once the code is chosen.
 */
object ColorCodeInsertion {

    /** Text that was selected when `&` was typed; completion wraps it with `&r` after the chosen code. */
    val PENDING_WRAP: Key<RangeMarker> = Key.create("cliColorHints.pendingWrap")

    /** The selection, if it lies completely inside one literal content range of [s]. */
    fun selectionInside(editor: Editor, s: ColorCodedString): TextRange? {
        val sel = editor.selectionModel
        if (!sel.hasSelection()) return null
        val r = TextRange(sel.selectionStart, sel.selectionEnd)
        return if (s.contentRanges().any { it.contains(r) }) r else null
    }
}
