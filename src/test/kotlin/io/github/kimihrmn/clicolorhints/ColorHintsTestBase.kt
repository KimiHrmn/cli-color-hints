package io.github.kimihrmn.clicolorhints

import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase

/** Fixture base: adds a stand-in `@ColorCoded` annotation and a fake library API. */
abstract class ColorHintsTestBase : LightJavaCodeInsightFixtureTestCase() {

    override fun getProjectDescriptor(): LightProjectDescriptor = JAVA_LATEST

    override fun setUp() {
        super.setUp()
        val pkg = ColorCodedTargeting.ANNOTATION_FQN.substringBeforeLast('.')
        val name = ColorCodedTargeting.ANNOTATION_FQN.substringAfterLast('.')
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

    protected val pkg: String get() = ColorCodedTargeting.ANNOTATION_FQN.substringBeforeLast('.')

    /** Java file importing the annotation and Cli. */
    protected fun java(body: String): String = "import $pkg.*;\n$body"

    /** Kotlin file importing the annotation and Cli. */
    protected fun kotlin(body: String): String = "import $pkg.*\n$body"

    protected fun findAtCaret(): ColorCodedString? =
        ColorCodedTargeting.find(myFixture.file.findElementAt(myFixture.caretOffset))
}
