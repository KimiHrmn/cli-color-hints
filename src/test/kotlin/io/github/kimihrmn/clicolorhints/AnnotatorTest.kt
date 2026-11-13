package io.github.kimihrmn.clicolorhints

import com.intellij.lang.annotation.HighlightSeverity

class AnnotatorTest : ColorHintsTestBase() {

    /** (highlighted text, foreground hex) for every forced-color highlight from our annotator. */
    private fun coloredRanges(fileName: String, text: String): List<Pair<String, String>> {
        myFixture.configureByText(fileName, text)
        val fileText = myFixture.file.text
        return myFixture.doHighlighting()
            .filter { it.severity == HighlightSeverity.INFORMATION && it.forcedTextAttributes?.foregroundColor != null }
            .sortedBy { it.startOffset }
            .map { fileText.substring(it.startOffset, it.endOffset) to ColorMath.hex(it.forcedTextAttributes!!.foregroundColor.rgb) }
    }

    private val yellow = ColorMath.hex(ColorCodes.byChar('6')!!.rgb!!)
    private val red = ColorMath.hex(ColorCodes.byChar('c')!!.rgb!!)

    fun testJavaColorsTextAndDimsCodes() {
        val r = coloredRanges("Test.java", java("class T { void m() { Cli.print(\"&6Hello&c!&r rest\"); } }"))
        assertEquals(listOf("&6", "Hello", "&c", "!", "&r"), r.map { it.first })
        assertEquals(yellow, r[1].second)
        assertEquals(red, r[3].second)
        // codes are dimmed: same color for all three tokens, not one of the palette colors
        assertEquals(1, setOf(r[0].second, r[2].second, r[4].second).size)
    }

    fun testJavaPlainStringNotColored() {
        assertEquals(emptyList<Pair<String, String>>(), coloredRanges("Test.java", java("class T { void m() { Cli.plain(\"&6Hello\"); } }")))
    }

    fun testJavaUnknownCodesNotColored() {
        assertEquals(emptyList<Pair<String, String>>(), coloredRanges("Test.java", java("class T { void m() { Cli.print(\"&x &l & A\"); } }")))
    }

    fun testJavaConcatenationCarriesColor() {
        val r = coloredRanges("Test.java", java("class T { void m(String n) { Cli.print(\"&6Hi \" + n + \"!\"); } }"))
        assertEquals(listOf("&6" to r[0].second, "Hi " to yellow, "!" to yellow), r)
    }

    fun testKotlinTemplate() {
        val r = coloredRanges("Test.kt", kotlin("fun m(name: String) { Cli.print(\"&cHi \$name!&r\") }"))
        assertEquals(listOf("&c", "Hi ", "!", "&r"), r.map { it.first })
        assertEquals(red, r[1].second)
        assertEquals(red, r[2].second)
    }

    fun testKotlinAnnotatedProperty() {
        val r = coloredRanges("Test.kt", kotlin("@ColorCoded val X = \"&aOK\""))
        assertEquals(listOf("&a", "OK"), r.map { it.first })
    }

    private fun addCommandAnnotation() {
        myFixture.addClass(
            """
            package $pkg;
            import java.lang.annotation.*;
            @Retention(RetentionPolicy.RUNTIME)
            @Target(ElementType.METHOD)
            public @interface Cmd {
                String[] value() default "";
                String usage() default "";
                @ColorCoded String description() default "";
                @ColorCoded String[] lines() default {};
            }
            """.trimIndent(),
        )
    }

    fun testJavaAnnotationAttributeColored() {
        addCommandAnnotation()
        val r = coloredRanges("Test.java", java("class T { @Cmd(value = \"&6x\", description = \"&cHi\") void m() {} }"))
        assertEquals(listOf("&c", "Hi"), r.map { it.first })
        assertEquals(red, r[1].second)
    }

    fun testJavaAnnotationAttributeArrayColored() {
        addCommandAnnotation()
        val r = coloredRanges("Test.java", java("class T { @Cmd(lines = {\"&6A\", \"&cB\"}) void m() {} }"))
        assertEquals(listOf("&6", "A", "&c", "B"), r.map { it.first })
    }

    fun testJavaAnnotationShorthandValueNotColored() {
        addCommandAnnotation()
        assertEquals(emptyList<Pair<String, String>>(), coloredRanges("Test.java", java("class T { @Cmd(\"&6x\") void m() {} }")))
    }

    fun testJavaAnnotationDefaultValueColored() {
        val r = coloredRanges(
            "Test.java",
            java("@interface Own { @ColorCoded String description() default \"&aOK\"; }"),
        )
        assertEquals(listOf("&a", "OK"), r.map { it.first })
    }

    fun testKotlinAnnotationAttributeColored() {
        addCommandAnnotation()
        val r = coloredRanges("Test.kt", kotlin("@Cmd(description = \"&cHi\") fun m() {}"))
        assertEquals(listOf("&c", "Hi"), r.map { it.first })
    }
}
