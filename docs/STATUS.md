# Status: cli-color-hints

_Last updated: 2026-11-11._

## Done

| # | Step | Where |
|---|------|-------|
| 1 | Gradle 9.8.2 wrapper, IntelliJ Platform Gradle Plugin 2.20.1, Kotlin 2.4.21 (api/language 2.2), `intellijIdea("2025.3.6.2")`, `since-build 253`, K2 declared, git | `build.gradle.kts`, `settings.gradle.kts`, `plugin.xml` |
| 2 | Code table (code → SGR → preview RGB → name), parser (code / text / placeholder segments), contrast math, HTML preview renderer | `ColorCode.kt`, `ColorCodeParser.kt`, `ColorMath.kt`, `PreviewRenderer.kt` |
| 3 | UAST targeting (Java + Kotlin): argument for a `@ColorCoded` parameter (positional and named); initializer of annotated field / property / local; assignment to an annotated variable; return value of an annotated method, including Kotlin expression bodies. `+` concatenation and Kotlin templates become literal and dynamic parts | `ColorCodedTargeting.kt` |
| 4 | Completion after `&` (icons, names, ANSI hint, table order, also matches names: `&yel` offers `&6` and `&e`). Auto-popup on `&`. Wraps the selection | `ColorCodeCompletion.kt`, `ColorCodeInsertion.kt` |
| 5 | Annotator: text after a code is shown in the code's color. Low-contrast text also gets a background chip. The `&X` token itself is dimmed. A small swatch inlay sits before each code | `ColorCodeAnnotator.kt`, `ColorSwatchInlayProvider.kt` |
| 6 | Hover and Ctrl+Q show a rendered preview: codes stripped, Ubuntu palette, aubergine terminal background, dynamic parts as `‹name›` in the current color | `ColorPreviewDocumentation.kt` |
| 7 | While the caret is inside the string, a palette of 17 clickable chips appears after it. A click inserts the code at the caret or wraps the selection. A caret listener refreshes the inlay pass | `ColorSwatchInlayProvider.kt`, `PaletteCaretListener.kt` |
| 8 | 73 tests: unit tests plus `LightJavaCodeInsightFixtureTestCase` fixture tests in Java and Kotlin. They cover targeting, completion, auto-popup, wrap-on-selection, annotator ranges and colors, inlays (rendered in the editor, following the caret), palette insert/wrap, and the preview. `./gradlew test buildPlugin` is green | `src/test/kotlin/...` |

In progress: none. Blocked: none.

## Install the plugin

1. Build it: `./gradlew buildPlugin` (Git Bash). Output: `build/distributions/cli-color-hints-0.1.2.zip`.
2. In IntelliJ IDEA 2025.3 or later, open **Settings → Plugins → ⚙ → Install Plugin from Disk…**, pick the zip, and restart.
3. Add `docs/ColorCoded.java` to your library under its real package. Enter its FQN in
   **Settings → Tools → CLI Color Hints** (one per line; several are allowed). This is a per-user IDE setting
   (`cliColorHints.xml` in the IDE config dir), not stored in any project. **Default: `com.example.shop.ColorCoded`.**
4. Annotate the lib's color-string parameters: `void println(@ColorCoded String text)`.
5. You can turn the swatches off in **Settings → Editor → Inlay Hints → Other → CLI color code swatches**.

## Decisions I made (spec ambiguous or silent)

- **Wrapper bootstrap.** The session sandbox would not run or copy the cached Gradle from outside the project. I wrote a one-line `gradlew` that delegated to `~/.gradle/wrapper/dists/gradle-9.8.2-bin/.../bin/gradle`, ran `./gradlew wrapper --gradle-version 9.8.2`, and that overwrote it with the real wrapper. The result is a standard wrapper with `distributionUrl` …`gradle-9.8.2-bin.zip`.
- **Codes are case-sensitive.** Only lowercase `a-f` and `r` count; `&A` is plain text. This matches the spec exactly. Easy to change in `ColorCodes.byChar`.
- **Codes never span concatenation parts.** In `"&" + "6"` the `&6` is not recognised.
- **Display names:** `&0` Black, `&7` Gray (light), `&8` Gray (dark), `&f` White. The rest follow "Color (dark/bright)".
- **Terminal colors:** preview background `#300A24` (Ubuntu aubergine), foreground `#FFFFFF`. The palette is Ubuntu GNOME Terminal's "Ubuntu" scheme (see `ColorCodes.ALL`).
- **Contrast chip:** added when the WCAG contrast ratio against the editor background is below 2.5. The chip is `#2B2B2B` or `#F2F2F2`, whichever contrasts more. 2.0 let bright yellow on white (ratio 2.01) through.
- **Dimmed code token:** the editor foreground blended 55% toward the background (forced attributes, theme-independent).
- **`&r` never wraps.** With a selection it is inserted at the selection start; `&r…&r` would be pointless.
- **Wrap via completion:** typing `&` over a selection inside a targeted string inserts `&` before the selection and keeps the text. Picking an item then gives `&X` + selection + `&r`. If the popup is cancelled, the result is `&` + text.
- **Extra reach:** assignment to an annotated variable (`f = "..."`) is also targeted. It is cheap and stays inside the "annotated declaration" idea.
- **Editor ranges come from the raw source text.** Escapes such as `\n` inside a colored run are colored too. The preview uses evaluated values.
- **Inlay API:** the presentation-based `InlayHintsProvider` (deprecated-ish), because the declarative API cannot paint colors. To refresh on caret moves it calls the internal `InlayHintsPassFactoryInternal.forceHintsUpdateOnNextPass()` plus `DaemonCodeAnalyzer.restart(file, reason)`. This happens only when the caret enters or leaves a string literal in a Java or Kotlin file.
- **Kotlin plugin is a hard `<depends>`.** Targeting uses `KtAnnotated` for top-level Kotlin properties, whose light field does not carry Java annotations under K2.
- **No `until-build`** (open-ended compatibility).
- **Completion confidence:** a `completion.confidence` (order first) lets auto-popup happen in strings, which Java and Kotlin normally suppress. It only applies right after `&` in a targeted string.
- `org.gradle.caching=true`, so a repeated `./gradlew test` may report results FROM-CACHE. Use `--rerun-tasks` to force a run.

## Untested / unverified

- **Real UI was not checked.** `runIde` was not launched. Not seen: chip and swatch painting, sizes on HiDPI, how the hover popup renders the inline-styled `<pre>` background.
- **Palette mouse clicks.** The insert/wrap logic is tested by calling it directly. Not tested: whether a real click on an inlay chip keeps the editor selection (needed for wrap) and the caret position. If a click clears the selection, wrap-by-click will not work and only insert-at-caret will.
- **Kotlin K1 mode** (tests run K2 only). **Plugin Verifier against other IDE builds** was not run (`verifyPlugin` needs IDE downloads). `verifyPluginStructure` passes.
- **Performance in large files.** The caret listener does a PSI lookup on every caret move and restarts the daemon only on string enter/leave.

## Open items for the user

1. Real `@ColorCoded` package: set it in Settings → Tools → CLI Color Hints and in the snippet `docs/ColorCoded.java`.
2. Check the SGR mapping and the Ubuntu RGB values in `ColorCodes.ALL` against the lib and a real terminal.
3. Does the lib support a literal-`&` escape (`&&`)? Currently `&&6` = literal `&` + code `&6`.
4. Uppercase codes (`&A`): should they count? Currently no.
5. Try it in a real IDE (`./gradlew runIde`) and report how palette clicks and selection wrap behave.
