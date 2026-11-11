// Paste into your library. Then set the same FQN in
// ColorCodedTargeting.ANNOTATION_FQN (src/main/kotlin/io/github/kimihrmn/clicolorhints/ColorCodedTargeting.kt)
// and rebuild the plugin.
package com.example.shop; // <- REPLACE with the lib's real package

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a String parameter, field, local variable or method return value whose text uses
 * console color codes ({@code &0}-{@code &9}, {@code &a}-{@code &f}, {@code &r}).
 *
 * <p>Only read by the "CLI Color Hints" IntelliJ plugin; it has no runtime effect.
 *
 * <pre>{@code
 * public void println(@ColorCoded String text) { ... }
 * }</pre>
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD, ElementType.LOCAL_VARIABLE})
public @interface ColorCoded {
}
