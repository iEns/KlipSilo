package com.phaprodevelop.klipsilo

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Static guard for the invisible break characters (#62, #64): the soft
 * hyphen U+00AD that the translation tooling inserts into long localized
 * DE/DA strings, and the zero-width space U+200B that the display rules
 * insert ([com.phaprodevelop.klipsilo.data.withDisplayBreaks],
 * `String.insertDisplayBreaksAfterPlusAndMinus`). Both are display-only —
 * TalkBack must read the clean source string — so the marks may only reach
 * a composables whose semantics are set from the clean source.
 *
 * The test parses every locale `strings.xml` (all `values*` resource
 * folders), collects the keys whose
 * value carries U+00AD/U+200B, and fails when such a key is used anywhere
 * outside a safe render path. It also fails when a display-break insertion
 * result is used outside a safe render path.
 *
 * Safe render paths:
 * - a call to a composable known to set clean semantics from the source
 *   string ([DisplayText], [ButtonLabel] and the wrappers built on them),
 * - a `R.string.x` usage directly followed by
 *   `.withoutSoftHyphens()` / `.withoutInvisibleBreaks()`,
 * - an explicit entry in [stringAllowlist] / [displayBreakAllowlist] with a
 *   reason.
 *
 * Heuristic limits (deliberately simple, plain JUnit, no new dependencies):
 * - The "enclosing call" is approximated by walking backwards from the
 *   usage, matching `(` / `)` and collecting the identifier before each
 *   unmatched `(` until a `{`, `}` or `;` at depth 0 ends the statement.
 *   A usage passed through an intermediate (data-class constructor, local
 *   variable, named constant) is NOT resolved to its render site — such
 *   sites need an allowlist entry whose reason names the render path.
 * - Comments and string literal contents are blanked out before matching,
 *   so a `(` inside a literal cannot confuse the paren walk; exotic Kotlin
 *   string templates are not modelled.
 * - Only `app/src/main/java` is scanned: test code renders nothing.
 */
class InvisibleBreakUsageTest {
    private val repoRoot: File =
        generateSequence(File(System.getProperty("user.dir") ?: ".").absoluteFile) { it.parentFile }
            .first { File(it, "app/src/main/res").isDirectory }

    /** Composables that render visible text with clean semantics (#64). */
    private val safeCalls =
        setOf(
            "DisplayText",
            "ButtonLabel",
            "DialogTitleLabel",
            "FitBackBar",
            "FitTopBar",
            "TopBarFitTitle",
            // Wrappers whose labels render through ButtonLabel/DisplayText:
            "GuideRow",
            "ToolRow",
            "SheetRow",
            "LanguageOptionRow",
            "PdfSettingSlider",
            "MarkdownOption",
            "CombineDialog",
        )

    private val safeStripSuffixes = listOf(".withoutSoftHyphens", ".withoutInvisibleBreaks")

    private val displayBreakFunctions =
        listOf("withDisplayBreaks", "insertDisplayBreaksAfterPlusAndMinus")

    /**
     * `R.string.<key>` usages the heuristic cannot resolve to a safe render
     * path, keyed by file name then string key. Each entry must name the
     * render path that keeps the semantics clean.
     */
    private val stringAllowlist: Map<String, Map<String, String>> =
        mapOf(
            // The nav label is consumed in AppRoot.kt: ButtonLabel for the
            // visible label, withoutInvisibleBreaks() for the icon-only
            // content description.
            "BottomNavItem.kt" to
                mapOf(
                    "settings" to
                        "nav label rendered through ButtonLabel in AppRoot.kt; icon-only " +
                        "content description stripped with withoutInvisibleBreaks()",
                ),
            // FormFillScreen.kt renders FormFieldInfo.label through
            // ButtonLabel (checkbox branch); the label never reaches a
            // content description, share, file name or PDF output.
            "ClipboardRepository.kt" to
                mapOf(
                    "form_field_checkbox" to
                        "form field label rendered through ButtonLabel in FormFillScreen.kt; " +
                        "no non-visible sink",
                ),
            // The strings flow into data-class fields; the render sites below
            // use DisplayText (tip.title / tip.body / category.title).
            "TipsScreen.kt" to
                mapOf(
                    "tips_cat_settings" to "rendered through DisplayText (category.title)",
                    "tips_cat_sheet" to "rendered through DisplayText (category.title)",
                    "tip_clips_auto_t" to "rendered through DisplayText (tip.title)",
                    "tip_pdf_pages_t" to "rendered through DisplayText (tip.title)",
                    "tip_settings_pdf_t" to "rendered through DisplayText (tip.title)",
                    "tip_tools_combine_t" to "rendered through DisplayText (tip.title)",
                ),
            "LimitationsScreen.kt" to
                mapOf(
                    "limitations_break_t" to "rendered through DisplayText (limitation.title)",
                    "limitations_spacing_t" to "rendered through DisplayText (limitation.title)",
                    "limitations_symbols_t" to "rendered through DisplayText (limitation.title)",
                    "limitations_summary_t" to "rendered through DisplayText (summary title)",
                ),
            "UseCasesScreen.kt" to
                mapOf(
                    "use_3_t" to "rendered through DisplayText (section.title)",
                    "use_7_t" to "rendered through DisplayText (section.title)",
                    "use_10_t" to "rendered through DisplayText (section.title)",
                ),
            "WhyScreen.kt" to
                mapOf("why_9_t" to "rendered through DisplayText (section.title)"),
            // The title val is rendered through DisplayText in the TopAppBar.
            "TextPresentationScreen.kt" to
                mapOf(
                    "text_presentation" to
                        "the title val is rendered through DisplayText in the TopAppBar",
                ),
            // The label is held in SheetItem and rendered through
            // SheetRow → DisplayText.
            "ClipActionSheet.kt" to
                mapOf(
                    "download_url" to "label rendered through SheetRow → DisplayText",
                    "text_presentation" to "label rendered through SheetRow → DisplayText",
                ),
            // The TopBarIconAction label: the icon's content description is
            // stripped with withoutSoftHyphens() (TopBarSaveAction.kt), the
            // tooltip and the overflow menu item render through DisplayText.
            "DetailScreen.kt" to
                mapOf(
                    "preview" to
                        "TopBarIconAction label: content description stripped, tooltip " +
                        "and overflow menu render through DisplayText",
                ),
        )

    /** Files whose display-break insertion results are verified by hand. */
    private val displayBreakAllowlist: Map<String, String> =
        mapOf(
            // The title val is rendered through ButtonLabel (compact row) and
            // DisplayText (non-compact row) — both set clean semantics.
            "KlipScreen.kt" to
                "the withDisplayBreaks result (title) renders through ButtonLabel " +
                "(compact) and DisplayText (non-compact)",
            // The display version renders in a Text whose semantics are
            // overridden with Modifier.fitTextSemantics (AboutScreen.kt).
            "AboutScreen.kt" to
                "the insertDisplayBreaksAfterPlusAndMinus result renders in a Text " +
                "carrying Modifier.fitTextSemantics",
        )

    /** Every string key whose value in ANY locale carries U+00AD or U+200B. */
    private fun markedStringKeys(): Set<String> {
        val keys = mutableSetOf<String>()
        val factory = DocumentBuilderFactory.newInstance()
        repoRoot
            .resolve("app/src/main/res")
            .listFiles { file -> file.isDirectory && file.name.startsWith("values") }
            .orEmpty()
            .forEach { dir ->
                val strings = dir.resolve("strings.xml")
                if (strings.isFile) {
                    val doc = factory.newDocumentBuilder().parse(strings)
                    val nodes = doc.getElementsByTagName("string")
                    for (index in 0 until nodes.length) {
                        val node = nodes.item(index)
                        if (
                            node.textContent.contains(CHAR_SOFT_HYPHEN) ||
                            node.textContent.contains(CHAR_ZERO_WIDTH_SPACE)
                        ) {
                            keys.add(node.attributes.getNamedItem("name").nodeValue)
                        }
                    }
                }
            }
        return keys
    }

    private fun mainSources(): List<File> =
        repoRoot
            .resolve("app/src/main/java")
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()

    @Test
    fun softHyphenStringsRenderThroughCleanSemanticsSites() {
        val keys = markedStringKeys()
        assertTrue(
            "no string carries U+00AD/U+200B in any values*/strings.xml — if the " +
                "translations were cleaned on purpose, relax this guard",
            keys.isNotEmpty(),
        )

        val violations = mutableListOf<String>()
        mainSources().forEach { file ->
            val code = stripCommentsAndStringContents(file.readText())
            keys.forEach { key ->
                indexOfAll(code, "R.string.$key") { matchStart, matchEnd ->
                    val line = code.take(matchStart).count { it == '\n' } + 1
                    val ok =
                        isFollowedByStripCall(code, matchEnd) ||
                            enclosingCalls(code, matchStart).any { it in safeCalls } ||
                            stringAllowlist[file.name]?.containsKey(key) == true
                    if (!ok) {
                        violations.add(
                            "${file.name}:$line renders R.string.$key outside a safe " +
                                "render path — use DisplayText/ButtonLabel, strip the " +
                                "marks, or add a reasoned allowlist entry",
                        )
                    }
                }
            }
        }
        if (violations.isNotEmpty()) fail(violations.joinToString("\n"))
    }

    @Test
    fun displayBreakInsertionsStayInsideSafeRenderPaths() {
        val violations = mutableListOf<String>()
        mainSources().forEach { file ->
            val code = stripCommentsAndStringContents(file.readText())
            displayBreakFunctions.forEach { name ->
                indexOfAll(code, name) { matchStart, _ ->
                    val line = code.take(matchStart).count { it == '\n' } + 1
                    // Skip the declaration itself ("fun withDisplayBreaks(").
                    if (!code.take(matchStart).endsWith("fun ")) {
                        val ok =
                            enclosingCalls(code, matchStart).any { it in safeCalls } ||
                                displayBreakAllowlist.containsKey(file.name)
                        if (!ok) {
                            violations.add(
                                "${file.name}:$line uses $name() outside a safe render " +
                                    "path — route the result through DisplayText/" +
                                    "ButtonLabel/fitTextSemantics or add a reasoned " +
                                    "allowlist entry",
                            )
                        }
                    }
                }
            }
        }
        if (violations.isNotEmpty()) fail(violations.joinToString("\n"))
    }

    /** Calls enclosing [from]: the identifier before each unmatched `(`, walking backwards. */
    private fun enclosingCalls(
        code: String,
        from: Int,
        limit: Int = 8,
    ): List<String> {
        val names = mutableListOf<String>()
        var depth = 0
        var index = from
        while (index >= 0 && names.size < limit) {
            when (val c = code[index]) {
                ')' -> depth++
                '(' -> {
                    if (depth == 0) {
                        var end = index - 1
                        while (end >= 0 && code[end].isWhitespace()) end--
                        var start = end
                        while (start >= 0 && (code[start].isLetterOrDigit() || code[start] == '_')) start--
                        val name = code.substring(start + 1, end + 1)
                        if (name.isEmpty()) break
                        names.add(name)
                    } else {
                        depth--
                    }
                }
                '{', '}', ';' -> if (depth == 0 && !c.isWhitespace()) return names
            }
            index--
        }
        return names
    }

    /** True when the usage is directly followed by `.withoutSoftHyphens()` / `.withoutInvisibleBreaks()`. */
    private fun isFollowedByStripCall(
        code: String,
        from: Int,
    ): Boolean {
        var index = from
        while (index < code.length && (code[index].isWhitespace() || code[index] == ')')) index++
        return safeStripSuffixes.any { code.startsWith(it, index) }
    }

    /** Blanks out comments and string literal contents, keeping offsets and newlines. */
    private fun stripCommentsAndStringContents(code: String): String {
        val out = StringBuilder(code.length)
        var index = 0
        var blockComment = false
        var lineComment = false
        while (index < code.length) {
            val c = code[index]
            val next = code.getOrNull(index + 1)
            when {
                blockComment ->
                    if (c == '*' && next == '/') {
                        out.append("  ")
                        index += 2
                        blockComment = false
                    } else {
                        if (c == '\n') out.append('\n')
                        index++
                    }
                lineComment ->
                    if (c == '\n') {
                        out.append('\n')
                        lineComment = false
                    } else {
                        index++
                    }
                c == '/' && next == '/' -> {
                    out.append("  ")
                    index += 2
                    lineComment = true
                }
                c == '/' && next == '*' -> {
                    out.append("  ")
                    index += 2
                    blockComment = true
                }
                c == '"' -> {
                    // Blank the literal contents (heuristic: no triple quotes,
                    // no template edge cases — enough for the paren walk).
                    out.append('"')
                    index++
                    while (index < code.length && code[index] != '"') {
                        if (code[index] == '\\') {
                            out.append("  ")
                            index += 2
                        } else {
                            if (code[index] == '\n') out.append('\n')
                            index++
                        }
                    }
                    if (index < code.length) {
                        out.append('"')
                        index++
                    }
                }
                else -> {
                    out.append(c)
                    index++
                }
            }
        }
        return out.toString()
    }

    private inline fun indexOfAll(
        code: String,
        needle: String,
        action: (matchStart: Int, matchEnd: Int) -> Unit,
    ) {
        var index = code.indexOf(needle)
        while (index >= 0) {
            val end = index + needle.length
            // Word boundary: "R.string.settings" must not match inside
            // "R.string.settings_pdf_export".
            val nextChar = code.getOrNull(end)
            if (nextChar == null || !(nextChar.isLetterOrDigit() || nextChar == '_')) {
                action(index, end)
            }
            index = code.indexOf(needle, index + 1)
        }
    }
}
