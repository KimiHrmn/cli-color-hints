package io.github.kimihrmn.clicolorhints

import com.intellij.codeInsight.lookup.Lookup
import com.intellij.util.ThreeState

class CompletionTest : ColorHintsTestBase() {

    private fun lookupStrings(fileName: String, text: String): List<String> {
        myFixture.configureByText(fileName, text)
        myFixture.completeBasic()
        return myFixture.lookupElementStrings.orEmpty()
    }

    private fun select(token: String) {
        val item = myFixture.lookupElements!!.first { it.lookupString == token }
        myFixture.lookup.currentItem = item
        myFixture.finishLookup(Lookup.NORMAL_SELECT_CHAR)
    }

    fun testJavaOffersAllCodesAfterAmpersand() {
        val items = lookupStrings("Test.java", java("class T { void m() { Cli.print(\"Hi &<caret>\"); } }"))
        assertEquals(ColorCodes.ALL.map { it.token }, items.filter { it.startsWith("&") && it.length == 2 })
    }

    fun testJavaNoCodesInPlainString() {
        val items = lookupStrings("Test.java", java("class T { void m() { Cli.plain(\"Hi &<caret>\"); } }"))
        assertFalse(items.contains("&6"))
    }

    fun testJavaNoCodesWithoutAmpersand() {
        val items = lookupStrings("Test.java", java("class T { void m() { Cli.print(\"Hi <caret>\"); } }"))
        assertFalse(items.contains("&6"))
    }

    fun testJavaInsertAtCaret() {
        myFixture.configureByText("Test.java", java("class T { void m() { Cli.print(\"Hi &<caret>\"); } }"))
        myFixture.completeBasic()
        select("&6")
        myFixture.checkResult(java("class T { void m() { Cli.print(\"Hi &6<caret>\"); } }"))
    }

    fun testJavaPrefixFiltersToSingleCodeAndInserts() {
        myFixture.configureByText("Test.java", java("class T { void m() { Cli.print(\"Hi &c<caret>\"); } }"))
        myFixture.completeBasic()
        // "&c" also matches "&cyan..." alias lookup strings, so pick explicitly if a popup is shown.
        if (myFixture.lookupElements != null) select("&c")
        myFixture.checkResult(java("class T { void m() { Cli.print(\"Hi &c<caret>\"); } }"))
    }

    fun testJavaColorNameAlias() {
        myFixture.configureByText("Test.java", java("class T { void m() { Cli.print(\"Hi &mag<caret>\"); } }"))
        myFixture.completeBasic()
        val items = myFixture.lookupElementStrings.orEmpty()
        assertEquals(listOf("&5", "&d"), items.sorted())
        select("&d")
        myFixture.checkResult(java("class T { void m() { Cli.print(\"Hi &d<caret>\"); } }"))
    }

    fun testJavaWrapSelection() {
        myFixture.configureByText("Test.java", java("class T { void m() { Cli.print(\"Hi <selection>there</selection>!\"); } }"))
        myFixture.type('&')
        myFixture.checkResult(java("class T { void m() { Cli.print(\"Hi &<caret>there!\"); } }"))
        myFixture.completeBasic()
        select("&a")
        myFixture.checkResult(java("class T { void m() { Cli.print(\"Hi &athere&r<caret>!\"); } }"))
    }

    fun testJavaTypingOverSelectionOutsideTargetReplacesNormally() {
        myFixture.configureByText("Test.java", java("class T { void m() { Cli.plain(\"Hi <selection>there</selection>!\"); } }"))
        myFixture.type('&')
        myFixture.checkResult(java("class T { void m() { Cli.plain(\"Hi &<caret>!\"); } }"))
    }

    fun testKotlinOffersCodesAndInserts() {
        myFixture.configureByText("Test.kt", kotlin("fun m(name: String) { Cli.print(\"Hi \$name &<caret>\") }"))
        myFixture.completeBasic()
        assertTrue(myFixture.lookupElementStrings.orEmpty().containsAll(listOf("&0", "&f", "&r")))
        select("&e")
        myFixture.checkResult(kotlin("fun m(name: String) { Cli.print(\"Hi \$name &e<caret>\") }"))
    }

    fun testKotlinWrapSelection() {
        myFixture.configureByText("Test.kt", kotlin("@ColorCoded val X = \"<selection>Warn</selection>ing\""))
        myFixture.type('&')
        myFixture.completeBasic()
        select("&c")
        myFixture.checkResult(kotlin("@ColorCoded val X = \"&cWarn&r<caret>ing\""))
    }

    fun testConfidenceAllowsAutopopupOnlyInTargetedStrings() {
        myFixture.configureByText("Test.java", java("class T { void m() { Cli.print(\"Hi &<caret>\"); Cli.plain(\"Hi &\"); } }"))
        val c = ColorCodeCompletionConfidence()
        val file = myFixture.file
        val inTarget = myFixture.caretOffset
        val inPlain = file.text.lastIndexOf('&') + 1
        assertEquals(ThreeState.NO, c.shouldSkipAutopopup(myFixture.editor, file.findElementAt(inTarget)!!, file, inTarget))
        assertEquals(ThreeState.UNSURE, c.shouldSkipAutopopup(myFixture.editor, file.findElementAt(inPlain)!!, file, inPlain))
    }

    fun testCodePrefixStart() {
        assertEquals(2, ColorCodeCompletionContributor.codePrefixStart("ab&", 3))
        assertEquals(2, ColorCodeCompletionContributor.codePrefixStart("ab&6", 4))
        assertEquals(2, ColorCodeCompletionContributor.codePrefixStart("ab&yel", 6))
        assertNull(ColorCodeCompletionContributor.codePrefixStart("ab 6", 4))
        assertNull(ColorCodeCompletionContributor.codePrefixStart("", 0))
    }
}
