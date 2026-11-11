package io.github.kimihrmn.clicolorhints

import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase

/** Fixture base: adds a stand-in `@ColorCoded` annotation and a fake library API. */
abstract class ColorHintsTestBase : LightJavaCodeInsightFixtureTestCase() {

    override fun getProjectDescriptor(): LightProjectDescriptor = JAVA_LATEST

    override fun setUp() {
        super.setUp()
        ColorHintsSettings.getInstance().annotationFqns = listOf(DEFAULT_FQN)
        val pkg = DEFAULT_FQN.substringBeforeLast('.')
        val name = DEFAULT_FQN.substringAfterLast('.')
        myFixture.addClass(
            """
            package $pkg;
            import java.lang.annotation.*;
            @Retention(RetentionPolicy.CLASS)
            @Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD, ElementType.LOCAL_VARIABLE})
            public @interface $name {}
            """.trimIndent(),
        )
        myFixture.addClass(
            """
            package $pkg;
            public class Cli {
                public static void print(@$name String text) {}
                public static void plain(String text) {}
                public static void print2(String plain, @$name String colored) {}
            }
            """.trimIndent(),
        )
    }

    override fun tearDown() {
        try {
            ColorHintsSettings.getInstance().annotationFqns = listOf(DEFAULT_FQN)
        } catch (e: Throwable) {
            addSuppressedException(e)
        } finally {
            super.tearDown()
        }
    }

    protected val pkg: String get() = DEFAULT_FQN.substringBeforeLast('.')

    companion object {
        const val DEFAULT_FQN: String = ColorHintsSettings.DEFAULT_ANNOTATION_FQN
    }

    /** Java file importing the annotation and Cli. */
    protected fun java(body: String): String = "import $pkg.*;\n$body"

    /** Kotlin file importing the annotation and Cli. */
    protected fun kotlin(body: String): String = "import $pkg.*\n$body"

    protected fun findAtCaret(): ColorCodedString? =
        ColorCodedTargeting.find(myFixture.file.findElementAt(myFixture.caretOffset))
}
