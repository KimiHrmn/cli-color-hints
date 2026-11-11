package io.github.kimihrmn.clicolorhints

import com.intellij.testFramework.fixtures.CompletionAutoPopupTester
import com.intellij.util.ThrowableRunnable

/** Typing `&` inside a color-coded string opens the completion popup by itself. */
class AutoPopupTest : ColorHintsTestBase() {

    private lateinit var tester: CompletionAutoPopupTester

    override fun setUp() {
        super.setUp()
        tester = CompletionAutoPopupTester(myFixture)
    }

    override fun runInDispatchThread(): Boolean = false

    override fun runTestRunnable(testRunnable: ThrowableRunnable<Throwable>) {
        tester.runWithAutoPopupEnabled(testRunnable)
    }

    private fun typeAmpersandIn(fileName: String, text: String): List<String> {
        myFixture.configureByText(fileName, text)
        tester.typeWithPauses("&")
        return myFixture.lookupElementStrings.orEmpty()
    }

    fun testJavaPopupOnAmpersand() {
        val items = typeAmpersandIn("Test.java", java("class T { void m() { Cli.print(\"Hi <caret>\"); } }"))
        assertTrue(items.toString(), items.containsAll(listOf("&6", "&r")))
    }

    fun testKotlinPopupOnAmpersand() {
        val items = typeAmpersandIn("Test.kt", kotlin("fun m() { Cli.print(\"Hi <caret>\") }"))
        assertTrue(items.toString(), items.containsAll(listOf("&6", "&r")))
    }

    fun testNoPopupInPlainString() {
        val items = typeAmpersandIn("Test.java", java("class T { void m() { Cli.plain(\"Hi <caret>\"); } }"))
        assertFalse(items.contains("&6"))
    }
}
