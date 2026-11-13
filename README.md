# CLI Color Hints (IntelliJ plugin)

Helps with Minecraft-style console color codes (`&0`-`&9`, `&a`-`&f`, `&r` → 4-bit ANSI) in Java and Kotlin strings
that reach a `@ColorCoded` parameter or declaration:

- completion popup on `&` (wraps a selection: `&X` + selection + `&r`)
- text colored in the editor; the `&X` tokens are hidden and shown while the caret is inside the string
- hover / Ctrl+Q: rendered terminal preview (`‹name›` for dynamic parts)

Build: `./gradlew test buildPlugin` → `cli-color-hints-0.1.4.zip` (Install Plugin from Disk, IDEA 2025.3+).

## Configure the annotation

The plugin only reacts to strings that reach an annotation you choose (by default `com.example.shop.ColorCoded`).
To point it at your own annotation:

1. Add the annotation to your library, e.g. by copying [`docs/ColorCoded.java`](docs/ColorCoded.java) and changing
   its `package` line. Any annotation works; the name doesn't have to be `ColorCoded`.
2. Annotate the color-string parameters or declarations: `void println(@ColorCoded String text)`.
   Annotation attributes work too: with `@ColorCoded String description();` declared in your own annotation,
   `@CliCommand(description = "&aHi")` is colored (arrays like `lines = {"&a1", "&c2"}` as well).
   The snippet in `docs/` already allows `ElementType.METHOD`, which is what attributes need.
3. In IntelliJ IDEA open **Settings → Tools → CLI Color Hints**.
4. Enter the annotation's fully qualified name, one per line, e.g. `org.acme.console.ColorCoded`.
   You can list several; a string counts if any of them matches. An empty list turns the plugin off.
5. Click **Apply**. Open editors refresh right away; no restart or rebuild needed.

This is a **per-user IDE setting**, saved as `cliColorHints.xml` in the IDE config directory
(e.g. `%APPDATA%\JetBrains\IntelliJIdea<version>\options\` on Windows,
`~/.config/JetBrains/IntelliJIdea<version>/options/` on Linux). It applies to all projects and is never written
into a project, so there is nothing to commit. It follows Settings Sync if you use it.

Setup and open items: see [`docs/STATUS.md`](docs/STATUS.md). Annotation snippet for the lib: [`docs/ColorCoded.java`](docs/ColorCoded.java).
Code → SGR → color table: `src/main/kotlin/io/github/kimihrmn/clicolorhints/ColorCode.kt`.
