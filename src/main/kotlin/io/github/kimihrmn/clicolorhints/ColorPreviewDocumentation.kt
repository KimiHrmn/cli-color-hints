package io.github.kimihrmn.clicolorhints

import com.intellij.model.Pointer
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.DocumentationTargetProvider
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager

/** Hover / Ctrl+Q on a color-coded string: rendered console preview (codes stripped, Ubuntu palette). */
class ColorPreviewDocumentationTargetProvider : DocumentationTargetProvider {
    override fun documentationTargets(file: PsiFile, offset: Int): List<DocumentationTarget> {
        val leaf = file.findElementAt(offset) ?: return emptyList()
        ColorCodedTargeting.find(leaf) ?: return emptyList()
        return listOf(ColorPreviewTarget(leaf))
    }
}

/** [anchor] is any PSI element inside the string literal; the preview is recomputed from it. */
class ColorPreviewTarget(anchor: PsiElement) : DocumentationTarget {
    private val pointer = SmartPointerManager.createPointer(anchor)

    override fun createPointer(): Pointer<out DocumentationTarget> {
        val p = pointer
        return Pointer { p.element?.let(::ColorPreviewTarget) }
    }

    override fun computePresentation(): TargetPresentation =
        TargetPresentation.builder("Console preview").presentation()

    override fun computeDocumentationHint(): String? = previewHtml()

    override fun computeDocumentation(): DocumentationResult? = previewHtml()?.let { DocumentationResult.documentation(it) }

    /** The rendered preview, or `null` if the string is no longer color-coded. */
    fun previewHtml(): String? {
        val s = ColorCodedTargeting.find(pointer.element) ?: return null
        return PreviewRenderer.toHtml(ColorCodeParser.parse(s.valueParts()))
    }
}
