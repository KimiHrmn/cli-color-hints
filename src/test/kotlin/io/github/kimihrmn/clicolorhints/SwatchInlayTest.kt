package io.github.kimihrmn.clicolorhints

import com.intellij.openapi.editor.Inlay

class SwatchInlayTest : ColorHintsTestBase() {

    private fun stringAtCaret(): ColorCodedString = findAtCaret()!!

    fun testCodeSwatchesAndPaletteWhenCaretInside() {
        val text = java("class T { void m() { Cli.print(\"&6Hi <caret>&cthere\"); } }")
        myFixture.configureByText("Test.java", text)
        val s = stringAtCaret()
        val hints = ColorSwatchInlayProvider.hintsFor(s, myFixture.caretOffset)
        val fileText = myFixture.file.text
        val swatches = hints.filterIsInstance<ColorSwatchInlayProvider.Hint.CodeSwatch>()
        assertEquals(listOf("&6", "&c"), swatches.map { fileText.substring(it.offset, it.offset + 2) })
        val palette = hints.filterIsInstance<ColorSwatchInlayProvider.Hint.Palette>().single()
        assertEquals(s.range!!.endOffset, palette.offset)
        assertEquals("\")", fileText.substring(palette.offset - 1, palette.offset + 1))
    }

    fun testNoPaletteWhenCaretOutside() {
        myFixture.configureByText("Test.java", java("class T { void m() { Cli.print(\"&6Hi\"); <caret> } }"))
        val s = ColorCodedTargeting.find(myFixture.file.findElementAt(myFixture.file.text.indexOf("&6")))!!
        val hints = ColorSwatchInlayProvider.hintsFor(s, myFixture.caretOffset)
        assertEquals(1, hints.size)
        assertTrue(hints.single() is ColorSwatchInlayProvider.Hint.CodeSwatch)
    }

    fun testConcatenationPaletteAfterWholeExpression() {
        myFixture.configureByText("Test.java", java("class T { void m(String n) { Cli.print(\"&6a<caret>\" + n + \"&cb\"); } }"))
        val s = stringAtCaret()
        val hints = ColorSwatchInlayProvider.hintsFor(s, myFixture.caretOffset)
        assertEquals(2, hints.count { it is ColorSwatchInlayProvider.Hint.CodeSwatch })
        val palette = hints.filterIsInstance<ColorSwatchInlayProvider.Hint.Palette>().single()
        assertEquals(myFixture.file.text.indexOf("\"&cb\"") + 5, palette.offset)
    }

    fun testKotlinSwatches() {
        myFixture.configureByText("Test.kt", kotlin("fun m(n: String) { Cli.print(\"&6a \$n &c<caret>b\") }"))
        val s = stringAtCaret()
        val hints = ColorSwatchInlayProvider.hintsFor(s, myFixture.caretOffset)
        val fileText = myFixture.file.text
        assertEquals(
            listOf("&6", "&c"),
            hints.filterIsInstance<ColorSwatchInlayProvider.Hint.CodeSwatch>().map { fileText.substring(it.offset, it.offset + 2) },
        )
        assertEquals(1, hints.count { it is ColorSwatchInlayProvider.Hint.Palette })
    }

    fun testInlaysRenderedInEditor() {
        myFixture.configureByText("Test.java", java("class T { void m() { Cli.print(\"&6Hi<caret>\"); } }"))
        myFixture.doHighlighting()
        val inlays: List<Inlay<*>> = myFixture.editor.inlayModel.getInlineElementsInRange(0, myFixture.editor.document.textLength)
        val rendered = inlays.joinToString(" ") { it.renderer.toString() }
        assertTrue("swatch before &6 in: $rendered", rendered.contains("■&6"))
        assertTrue("palette in: $rendered", rendered.contains("[&f]") && rendered.contains("[&r]"))
    }

    fun testPaletteFollowsCaret() {
        myFixture.configureByText("Test.java", java("class T { void m() { <caret>Cli.print(\"&6Hi\"); } }"))
        fun rendered(): String {
            myFixture.doHighlighting()
            return myFixture.editor.inlayModel.getInlineElementsInRange(0, myFixture.editor.document.textLength)
                .joinToString(" ") { it.renderer.toString() }
        }
        assertFalse("no palette while caret outside: ${rendered()}", rendered().contains("[&f]"))
        myFixture.editor.caretModel.moveToOffset(myFixture.file.text.indexOf("Hi"))
        assertTrue("palette after caret entered: ${rendered()}", rendered().contains("[&f]"))
        myFixture.editor.caretModel.moveToOffset(myFixture.file.text.indexOf("Cli"))
        assertFalse("palette gone after caret left: ${rendered()}", rendered().contains("[&f]"))
        assertTrue("code swatch stays", rendered().contains("■&6"))
    }

    // ---- palette click behavior ----

    fun testPaletteClickInsertsAtCaret() {
        myFixture.configureByText("Test.java", java("class T { void m() { Cli.print(\"Hi <caret>there\"); } }"))
        ColorCodeInsertion.apply(project, myFixture.editor, ColorCodes.byChar('b')!!)
        myFixture.checkResult(java("class T { void m() { Cli.print(\"Hi &b<caret>there\"); } }"))
    }

    fun testPaletteClickWrapsSelection() {
        myFixture.configureByText("Test.java", java("class T { void m() { Cli.print(\"Hi <selection>there</selection>!\"); } }"))
        ColorCodeInsertion.apply(project, myFixture.editor, ColorCodes.byChar('4')!!)
        myFixture.checkResult(java("class T { void m() { Cli.print(\"Hi &4there&r<caret>!\"); } }"))
    }

    fun testPaletteResetWithSelectionInsertsAtStart() {
        myFixture.configureByText("Test.java", java("class T { void m() { Cli.print(\"&6Hi <selection>there</selection>!\"); } }"))
        ColorCodeInsertion.apply(project, myFixture.editor, ColorCodes.RESET)
        myFixture.checkResult(java("class T { void m() { Cli.print(\"&6Hi &r<caret>there!\"); } }"))
    }

    fun testPaletteClickKotlinWrap() {
        myFixture.configureByText("Test.kt", kotlin("fun m() { Cli.print(\"<selection>Error</selection>: x\") }"))
        ColorCodeInsertion.apply(project, myFixture.editor, ColorCodes.byChar('c')!!)
        myFixture.checkResult(kotlin("fun m() { Cli.print(\"&cError&r<caret>: x\") }"))
    }

    fun testPaletteClickOutsideTargetDoesNothing() {
        val text = java("class T { void m() { Cli.plain(\"Hi <caret>there\"); } }")
        myFixture.configureByText("Test.java", text)
        ColorCodeInsertion.apply(project, myFixture.editor, ColorCodes.byChar('b')!!)
        myFixture.checkResult(text)
    }
}
