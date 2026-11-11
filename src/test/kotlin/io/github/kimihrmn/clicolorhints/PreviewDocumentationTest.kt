package io.github.kimihrmn.clicolorhints

import com.intellij.platform.backend.documentation.DocumentationTargetProvider

class PreviewDocumentationTest : ColorHintsTestBase() {

    private fun previewAtCaret(): String? {
        val targets = ColorPreviewDocumentationTargetProvider().documentationTargets(myFixture.file, myFixture.caretOffset)
        return (targets.singleOrNull() as? ColorPreviewTarget)?.previewHtml()
    }

    private val yellow = ColorMath.hex(ColorCodes.byChar('6')!!.rgb!!)
    private val red = ColorMath.hex(ColorCodes.byChar('c')!!.rgb!!)

    fun testJavaPreview() {
        myFixture.configureByText("Test.java", java("class T { void m() { Cli.print(\"&6Hel<caret>lo&c!&r\\r\\n\"); } }"))
        val html = previewAtCaret()!!
        assertTrue(html, html.contains("<span style=\"color:$yellow;\">Hello</span><span style=\"color:$red;\">!</span>"))
        assertFalse("codes stripped", html.contains("&6") || html.contains("&amp;6"))
        assertFalse("no raw escapes", html.contains("\\r") || html.contains("\\n") || html.contains("\u001b"))
        assertTrue("terminal background", html.contains("background-color:#300A24"))
    }

    fun testJavaConcatPlaceholderCarriesColor() {
        myFixture.configureByText("Test.java", java("class T { void m(String name) { Cli.print(\"&6Hi<caret> \" + name + \"&r!\"); } }"))
        val html = previewAtCaret()!!
        assertTrue(html, html.contains("<span style=\"color:$yellow;\">‹name›</span>"))
    }

    fun testKotlinTemplatePlaceholders() {
        myFixture.configureByText("Test.kt", kotlin("class U(val name: String)\nfun m(u: U) { Cli.print(\"&cHi<caret> \${u.name}!\") }"))
        val html = previewAtCaret()!!
        assertTrue(html, html.contains("<span style=\"color:$red;\">‹u.name›</span>"))
    }

    fun testNoTargetForPlainString() {
        myFixture.configureByText("Test.java", java("class T { void m() { Cli.plain(\"&6Hel<caret>lo\"); } }"))
        assertTrue(ColorPreviewDocumentationTargetProvider().documentationTargets(myFixture.file, myFixture.caretOffset).isEmpty())
    }

    fun testProviderIsRegistered() {
        assertTrue(DocumentationTargetProvider.EP_NAME.extensionList.any { it is ColorPreviewDocumentationTargetProvider })
    }
}
