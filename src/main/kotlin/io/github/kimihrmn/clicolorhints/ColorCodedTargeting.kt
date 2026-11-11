package io.github.kimihrmn.clicolorhints

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UParenthesizedExpression
import org.jetbrains.uast.UPolyadicExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.UReturnExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.UastBinaryOperator
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.tryResolve

/** A color-coded string expression found in source. */
class ColorCodedString(
    /** Outermost string expression (concatenation / template / literal). */
    val expression: UExpression,
    /** Literal and dynamic parts, in order. */
    val parts: List<SourcePart>,
) {
    /** Text range of the whole expression in the file. */
    val range: TextRange? get() = expression.sourcePsi?.textRange

    /** Literal parts with their raw source text and absolute offsets: for editor features. */
    fun rawParts(): List<StringPart> = parts.map {
        when (it) {
            is SourcePart.Literal -> StringPart.Text(it.raw, it.rawRange?.startOffset)
            is SourcePart.Dynamic -> StringPart.Dynamic(it.name)
        }
    }

    /** Parts with evaluated literal values: for the rendered preview. */
    fun valueParts(): List<StringPart> = parts.map {
        when (it) {
            is SourcePart.Literal -> StringPart.Text(it.value)
            is SourcePart.Dynamic -> StringPart.Dynamic(it.name)
        }
    }

    /** Ranges of literal content (between quotes) in the file. */
    fun contentRanges(): List<TextRange> = parts.mapNotNull { (it as? SourcePart.Literal)?.rawRange }

    /** Range of the literal content that contains [offset] (start/end inclusive), if any. */
    fun contentRangeAt(offset: Int): TextRange? = contentRanges().firstOrNull { offset >= it.startOffset && offset <= it.endOffset }
}

sealed interface SourcePart {
    /**
     * @property raw source text between the quotes (escapes not decoded)
     * @property value evaluated string value
     * @property rawRange absolute range of [raw] in the file
     */
    data class Literal(val raw: String, val value: String, val rawRange: TextRange?) : SourcePart

    data class Dynamic(val name: String) : SourcePart
}

/**
 * Decides whether a string literal belongs to a `@ColorCoded` site. Java and Kotlin via UAST.
 *
 * Supported sites (no data flow):
 *  - argument passed to a `@ColorCoded` parameter
 *  - initializer of a `@ColorCoded` field / property / local variable / parameter default
 *  - value assigned to a `@ColorCoded` variable
 *  - returned from a `@ColorCoded` method / function (incl. Kotlin expression bodies)
 */
object ColorCodedTargeting {

    /**
     * FQN of the annotation in your library. PLACEHOLDER: replace with the real package.
     * See `docs/ColorCoded.java` for the snippet to add to the lib.
     */
    const val ANNOTATION_FQN: String = "com.example.shop.ColorCoded"

    /** Finds the color-coded string containing [element] (any PSI inside a string literal), or `null`. */
    fun find(element: PsiElement?): ColorCodedString? {
        val host = PsiTreeUtil.getParentOfType(element, PsiLanguageInjectionHost::class.java, false) ?: return null
        val top = topStringExpression(host) ?: return null
        if (!isColorCodedSite(top)) return null
        return ColorCodedString(top, flatten(top))
    }

    /** Same as [find] but without the annotation check (the string the caret is in). */
    fun stringHostRange(element: PsiElement?): TextRange? =
        PsiTreeUtil.getParentOfType(element, PsiLanguageInjectionHost::class.java, false)?.textRange

    /** The outermost string expression (literal, template or `+` concatenation) around [host]. */
    fun topStringExpression(host: PsiElement): UExpression? {
        var expr: UExpression = host.toUElement() as? UExpression ?: return null
        if (expr is ULiteralExpression && expr.value !is String) return null
        while (true) {
            val parent = expr.uastParent
            expr = when {
                parent is UParenthesizedExpression -> parent
                parent is UPolyadicExpression && isStringConcat(parent) -> parent
                else -> return expr
            }
        }
    }

    private fun isStringConcat(e: UPolyadicExpression): Boolean {
        if (e is UBinaryExpression && e.operator != UastBinaryOperator.PLUS) return false
        if (e.operator != UastBinaryOperator.PLUS) return false
        val type = e.getExpressionType()?.canonicalText
        if (type == "java.lang.String" || type == "kotlin.String") return true
        // Kotlin templates may report no type in some modes; fall back to "has a string literal operand".
        return type == null && e.operands.any { it is ULiteralExpression && it.value is String }
    }

    fun flatten(expr: UExpression): List<SourcePart> {
        val out = mutableListOf<SourcePart>()
        fun visit(e: UExpression) {
            when {
                e is UParenthesizedExpression -> visit(e.expression)
                e is UPolyadicExpression && isStringConcat(e) -> e.operands.forEach(::visit)
                e is ULiteralExpression && e.value is String -> out += literalPart(e)
                else -> out += SourcePart.Dynamic(placeholderName(e))
            }
        }
        visit(expr)
        return out
    }

    private fun literalPart(e: ULiteralExpression): SourcePart.Literal {
        val value = e.value as String
        val psi = e.sourcePsi ?: return SourcePart.Literal(value, value, null)
        val text = psi.text
        val quote = when {
            text.startsWith("\"\"\"") -> 3
            text.startsWith("\"") -> 1
            else -> 0
        }
        val endQuote = if (quote > 0 && text.length >= 2 * quote && text.endsWith("\"".repeat(quote))) quote else 0
        val start = psi.textRange.startOffset + quote
        val end = psi.textRange.endOffset - endQuote
        return if (end >= start) {
            SourcePart.Literal(text.substring(quote, text.length - endQuote), value, TextRange(start, end))
        } else {
            SourcePart.Literal(value, value, null)
        }
    }

    private fun placeholderName(e: UExpression): String = when (e) {
        is USimpleNameReferenceExpression -> e.identifier
        is UQualifiedReferenceExpression -> e.sourcePsi?.text ?: e.asSourceString()
        else -> e.sourcePsi?.text ?: e.asSourceString()
    }.trim().removePrefix("\${").removePrefix("$").removeSuffix("}").trim()

    /** Is the top-level string expression [expr] in a `@ColorCoded` position? */
    fun isColorCodedSite(expr: UExpression): Boolean {
        val parent = expr.uastParent ?: return false
        when (parent) {
            is UCallExpression -> return isAnnotatedParameterArgument(parent, expr)
            is UVariable -> return parent.uastInitializer.sameAs(expr) && isAnnotatedU(parent)
            is UBinaryExpression -> {
                if (parent.operator == UastBinaryOperator.ASSIGN && parent.rightOperand.sameAs(expr)) {
                    val target = parent.leftOperand.tryResolve() as? PsiModifierListOwner ?: return false
                    return isAnnotated(target)
                }
                return false
            }
            is UReturnExpression -> {
                val method = parent.jumpTarget as? UMethod ?: parent.getParentOfType(UMethod::class.java) ?: return false
                return isAnnotatedU(method)
            }
            is UMethod -> return isAnnotatedU(parent) // Kotlin expression body without implicit return
            else -> {
                // Kotlin expression body: method -> (implicit) return -> expr, or method -> block -> expr
                val method = expr.getParentOfType(UMethod::class.java, true) ?: return false
                val body = method.uastBody
                if (body != null && (body.sameAs(expr) || body.sourcePsi == expr.sourcePsi)) return isAnnotatedU(method)
                return false
            }
        }
    }

    private fun isAnnotatedParameterArgument(call: UCallExpression, arg: UExpression): Boolean {
        val method: PsiMethod = call.resolve() ?: return false
        val params = method.parameterList.parameters
        for ((i, p) in params.withIndex()) {
            val mapped = call.getArgumentForParameter(i) ?: continue
            if (mapped.sameAs(arg) && isAnnotated(p)) return true
        }
        // Fallback: positional match (e.g. when argument mapping is unavailable).
        val idx = call.valueArguments.indexOfFirst { it.sameAs(arg) }
        return idx in params.indices && call.valueArgumentCount == params.size && isAnnotated(params[idx])
    }

    fun isAnnotated(owner: PsiModifierListOwner): Boolean {
        if (owner.hasAnnotation(ANNOTATION_FQN)) return true
        val u = owner.toUElement()
        return u is UAnnotated && u.findAnnotation(ANNOTATION_FQN) != null
    }

    private fun isAnnotatedU(u: UElement): Boolean {
        if (u is UAnnotated && u.findAnnotation(ANNOTATION_FQN) != null) return true
        val psi = u.javaPsi as? PsiModifierListOwner
        if (psi != null && psi.hasAnnotation(ANNOTATION_FQN)) return true
        // Kotlin properties: the light field may not carry the annotation, so check the source entries.
        val kt = u.sourcePsi as? KtAnnotated ?: return false
        return kt.annotationEntries.any { (it.toUElement() as? UAnnotation)?.qualifiedName == ANNOTATION_FQN }
    }

    private fun UExpression?.sameAs(other: UExpression): Boolean {
        if (this == null) return false
        var a: UExpression = this
        while (a is UParenthesizedExpression) a = a.expression
        if (a == other) return true
        val pa = a.sourcePsi ?: return false
        return pa == other.sourcePsi
    }
}
