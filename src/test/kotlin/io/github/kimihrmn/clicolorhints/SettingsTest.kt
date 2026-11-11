package io.github.kimihrmn.clicolorhints

class SettingsTest : ColorHintsTestBase() {

    private fun addCustomAnnotation() {
        myFixture.addClass(
            """
            package org.acme.term;
            public @interface Ansi {}
            """.trimIndent(),
        )
        myFixture.addClass(
            """
            package org.acme.term;
            public class Out { public static void say(@Ansi String text) {} }
            """.trimIndent(),
        )
    }

    private fun targetedAt(body: String): Boolean {
        myFixture.configureByText("Test.java", "import org.acme.term.*;\nimport $pkg.*;\n$body")
        return findAtCaret() != null
    }

    fun testCustomFqnReplacesDefault() {
        addCustomAnnotation()
        ColorHintsSettings.getInstance().annotationFqns = listOf("org.acme.term.Ansi")
        assertTrue(targetedAt("class T { void m() { Out.say(\"&6Hi<caret>\"); } }"))
        assertFalse(targetedAt("class T { void m() { Cli.print(\"&6Hi<caret>\"); } }"))
    }

    fun testSeveralFqns() {
        addCustomAnnotation()
        ColorHintsSettings.getInstance().annotationFqns = listOf("org.acme.term.Ansi", DEFAULT_FQN)
        assertTrue(targetedAt("class T { void m() { Out.say(\"&6Hi<caret>\"); } }"))
        assertTrue(targetedAt("class T { void m() { Cli.print(\"&6Hi<caret>\"); } }"))
    }

    fun testCustomFqnInKotlin() {
        addCustomAnnotation()
        ColorHintsSettings.getInstance().annotationFqns = listOf("org.acme.term.Ansi")
        myFixture.configureByText("Test.kt", "import org.acme.term.*\n@Ansi val x = \"&6<caret>x\"")
        assertNotNull(findAtCaret())
    }

    fun testEmptyListTargetsNothing() {
        ColorHintsSettings.getInstance().annotationFqns = emptyList()
        assertFalse(targetedAt("class T { void m() { Cli.print(\"&6Hi<caret>\"); } }"))
    }

    fun testFqnsAreNormalized() {
        val s = ColorHintsSettings.getInstance()
        s.annotationFqns = listOf("  a.B ", "", "a.B", "c.D", "   ")
        assertEquals(listOf("a.B", "c.D"), s.annotationFqns)
    }
}
