package io.github.kimihrmn.clicolorhints

class TargetingTest : ColorHintsTestBase() {

    private fun javaAt(body: String): ColorCodedString? {
        myFixture.configureByText("Test.java", java(body))
        return findAtCaret()
    }

    private fun kotlinAt(body: String): ColorCodedString? {
        myFixture.configureByText("Test.kt", kotlin(body))
        return findAtCaret()
    }

    // ---- Java ----

    fun testJavaArgumentOfAnnotatedParameter() {
        assertNotNull(javaAt("class T { void m() { Cli.print(\"&6Hi<caret>\"); } }"))
    }

    fun testJavaArgumentOfPlainParameterIgnored() {
        assertNull(javaAt("class T { void m() { Cli.plain(\"&6Hi<caret>\"); } }"))
    }

    fun testJavaSecondParameterOnly() {
        assertNull(javaAt("class T { void m() { Cli.print2(\"&6<caret>x\", \"y\"); } }"))
        assertNotNull(javaAt("class T { void m() { Cli.print2(\"x\", \"&6<caret>y\"); } }"))
    }

    fun testJavaAnnotatedFieldInitializer() {
        assertNotNull(javaAt("class T { @ColorCoded static final String X = \"&6<caret>x\"; }"))
        assertNull(javaAt("class T { static final String X = \"&6<caret>x\"; }"))
    }

    fun testJavaAnnotatedLocalVariable() {
        assertNotNull(javaAt("class T { void m() { @ColorCoded String s = \"&6<caret>x\"; } }"))
    }

    fun testJavaAnnotatedMethodReturn() {
        assertNotNull(javaAt("class T { @ColorCoded String m() { return \"&6<caret>x\"; } }"))
        assertNull(javaAt("class T { String m() { return \"&6<caret>x\"; } }"))
    }

    fun testJavaAssignmentToAnnotatedField() {
        assertNotNull(javaAt("class T { @ColorCoded String f; void m() { f = \"&6<caret>x\"; } }"))
    }

    fun testJavaConcatenationParts() {
        val s = javaAt("class T { void m(String name) { Cli.print(\"&6Hi \" + name + \"!<caret>\"); } }")!!
        assertEquals(
            listOf(StringPart.Text("&6Hi "), StringPart.Dynamic("name"), StringPart.Text("!")),
            s.valueParts(),
        )
        assertEquals(2, s.contentRanges().size)
    }

    fun testJavaRawRangesPointAtContent() {
        val s = javaAt("class T { void m() { Cli.print(\"&6Hi<caret>\\n\"); } }")!!
        val part = s.rawParts().single() as StringPart.Text
        assertEquals("&6Hi\\n", part.text)
        assertEquals("&6Hi\\n", myFixture.file.text.substring(part.offset!!, part.offset!! + part.text.length))
        assertEquals("&6Hi\n", (s.valueParts().single() as StringPart.Text).text)
    }

    // ---- Kotlin ----

    fun testKotlinCallToJavaAnnotatedParameter() {
        assertNotNull(kotlinAt("fun m() { Cli.print(\"&6Hi<caret>\") }"))
        assertNull(kotlinAt("fun m() { Cli.plain(\"&6Hi<caret>\") }"))
    }

    fun testKotlinAnnotatedKotlinParameter() {
        assertNotNull(kotlinAt("fun out(@ColorCoded s: String) {}\nfun m() { out(\"&6<caret>x\") }"))
        assertNull(kotlinAt("fun out(s: String) {}\nfun m() { out(\"&6<caret>x\") }"))
    }

    fun testKotlinNamedArgument() {
        assertNotNull(kotlinAt("fun out(a: String, @ColorCoded s: String) {}\nfun m() { out(s = \"&6<caret>x\", a = \"b\") }"))
    }

    fun testKotlinTemplateParts() {
        val s = kotlinAt("fun m(name: String) { Cli.print(\"&6Hi \$name!<caret>\") }")!!
        assertEquals(
            listOf(StringPart.Text("&6Hi "), StringPart.Dynamic("name"), StringPart.Text("!")),
            s.valueParts().filterNot { it is StringPart.Text && it.text.isEmpty() },
        )
    }

    fun testKotlinAnnotatedPropertyAndLocal() {
        assertNotNull(kotlinAt("@ColorCoded val X = \"&6<caret>x\""))
        assertNotNull(kotlinAt("fun m() { @ColorCoded val s = \"&6<caret>x\" }"))
        assertNull(kotlinAt("val X = \"&6<caret>x\""))
    }

    fun testKotlinAnnotatedFunctionReturn() {
        assertNotNull(kotlinAt("@ColorCoded fun g(): String { return \"&6<caret>x\" }"))
        assertNotNull(kotlinAt("@ColorCoded fun g() = \"&6<caret>x\""))
        assertNull(kotlinAt("fun g() = \"&6<caret>x\""))
    }
}
