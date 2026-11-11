# CLI Color Hints (IntelliJ plugin)

Helps with Minecraft-style console color codes (`&0`-`&9`, `&a`-`&f`, `&r` → 4-bit ANSI) in Java and Kotlin strings
that reach a `@ColorCoded` parameter or declaration:

- completion popup on `&` (wraps a selection: `&X` + selection + `&r`)
- text colored in the editor, code tokens dimmed, swatch before each code
- clickable 17-color palette after the string while the caret is inside it
- hover / Ctrl+Q: rendered terminal preview (`‹name›` for dynamic parts)

Build: `./gradlew test buildPlugin` → `build/distributions/cli-color-hints-0.1.2.zip` (Install Plugin from Disk, IDEA 2025.3+).

Setup and open items: see [`docs/STATUS.md`](docs/STATUS.md). Annotation snippet for the lib: [`docs/ColorCoded.java`](docs/ColorCoded.java).
Code → SGR → color table: `src/main/kotlin/io/github/kimihrmn/clicolorhints/ColorCode.kt`.
