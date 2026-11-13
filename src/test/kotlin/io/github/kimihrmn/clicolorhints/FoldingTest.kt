package io.github.kimihrmn.clicolorhints

import com.intellij.openapi.editor.FoldRegion

/**
 * The fixture's folding pass is not the IDE's initial pass (new regions come up expanded), so the initial
 * collapsed state is checked on the builder directly and the caret-driven toggling on the live regions.
 */
class FoldingTest : ColorHintsTestBase() {

    private fun open(fileName: String, text: String) {
        myFixture.configureByText(fileName, text)
        myFixture.doHighlighting() // runs the folding pass
    }

    private fun codeRegions(): List<FoldRegion> =
        myFixture.editor.foldingModel.allFoldRegions.filter { it.placeholderText == ColorCodeFoldingBuilder.PLACEHOLDER }
            .sortedBy { it.startOffset }

    private fun regionTexts(): List<String> = codeRegions().map { myFixture.editor.document.getText(it.textRange) }

    private fun collapsedTexts(): List<String> =
        codeRegions().filter { !it.isExpanded }.map { myFixture.editor.document.getText(it.textRange) }

    private fun moveCaretTo(marker: String) {
        myFixture.editor.caretModel.moveToOffset(myFixture.file.text.indexOf(marker))
    }

    private fun collapsedByDefaultAtAmpersandSix(): Boolean {
        val host = ColorCodedTargeting.stringHostRange(myFixture.file.findElementAt(myFixture.file.text.indexOf("&6")))!!
        val node = myFixture.file.findElementAt(host.startOffset + 1)!!.parent.node
        return ColorCodeFoldingBuilder().isCollapsedByDefault(node)
    }

    fun testOneRegionPerKnownCode() {
        open("Test.java", java("class T { void m() { Cli.print(\"&6Hi &x &cthere\"); } }"))
        assertEquals(listOf("&6", "&c"), regionTexts())
    }

    fun testPlainStringHasNoRegions() {
        open("Test.java", java("class T { void m() { Cli.plain(\"&6Hi\"); } }"))
        assertEquals(0, codeRegions().size)
    }

    fun testConcatenationFoldsEveryLiteral() {
        open("Test.java", java("class T { void m(String n) { Cli.print(\"&6a\" + n + \"&cb\"); } }"))
        assertEquals(listOf("&6", "&c"), regionTexts())
    }

    fun testKotlinRegions() {
        open("Test.kt", kotlin("fun m() { Cli.print(\"&cHi\") }"))
        assertEquals(listOf("&c"), regionTexts())
    }

    fun testCollapsedByDefaultWhenCaretOutside() {
        open("Test.java", java("class T { void m() { <caret>Cli.print(\"&6Hi\"); } }"))
        assertTrue(collapsedByDefaultAtAmpersandSix())
    }

    fun testExpandedByDefaultWhenCaretInside() {
        open("Test.java", java("class T { void m() { Cli.print(\"&6H<caret>i\"); } }"))
        assertFalse(collapsedByDefaultAtAmpersandSix())
    }

    fun testCaretLeavingStringCollapsesAndEnteringExpands() {
        open("Test.java", java("class T { void m() { <caret>Cli.print(\"&6Hi &cthere\"); } }"))
        moveCaretTo("Hi")
        assertEquals(emptyList<String>(), collapsedTexts())
        moveCaretTo("Cli")
        assertEquals(listOf("&6", "&c"), collapsedTexts())
        moveCaretTo("there")
        assertEquals(emptyList<String>(), collapsedTexts())
    }

    fun testOnlyTheLeftStringCollapses() {
        open("Test.java", java("class T { void m() { Cli.print(\"&6A\"); Cli.print(\"&cB\"); } }"))
        moveCaretTo("A")
        moveCaretTo("B")
        assertEquals(listOf("&6"), collapsedTexts())
    }
}
