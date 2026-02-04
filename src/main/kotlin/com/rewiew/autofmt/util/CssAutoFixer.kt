package com.rewiew.autofmt.util

object CssAutoFixer {
    data class FixStats(
        var addedColons: Int = 0,
        var addedSemicolons: Int = 0,
        var addedBraces: Int = 0
    )

    fun applyAll(text: String): Pair<String, FixStats> {
        val stats = FixStats()
        var updated = fixMissingColonsAndSemicolons(text, stats)
        updated = fixMissingClosingBraces(updated, stats)
        return updated to stats
    }

    private fun fixMissingColonsAndSemicolons(text: String, stats: FixStats): String {
        val lines = text.split("\n")
        val out = ArrayList<String>(lines.size)

        var depth = 0
        var inBlockComment = false

        for (rawLine in lines) {
            var line = rawLine

            val (stripped, commentState) = stripBlockComments(line, inBlockComment)
            inBlockComment = commentState
            val trimmed = stripped.trim()

            val canFix = depth > 0 && trimmed.isNotBlank() && !trimmed.startsWith("@")
            if (canFix && !trimmed.contains("{") && !trimmed.contains("}") && !trimmed.startsWith("//") && !trimmed.startsWith("/*")) {
                if (!trimmed.contains(":")) {
                    val m = Regex("^([a-zA-Z-]+)\\s+(.+)$").matchEntire(trimmed)
                    if (m != null) {
                        val prop = m.groupValues[1]
                        val value = m.groupValues[2].trimEnd()
                        val indent = rawLine.takeWhile { it.isWhitespace() }
                        var fixed = "$prop: $value"
                        if (!fixed.trimEnd().endsWith(";")) {
                            fixed += ";"
                            stats.addedSemicolons++
                        }
                        stats.addedColons++
                        line = indent + fixed
                    }
                } else if (!trimmed.endsWith(";") && !trimmed.endsWith("{")) {
                    line = rawLine.trimEnd() + ";"
                    stats.addedSemicolons++
                }
            }

            out.add(line)

            // Update depth based on stripped content (ignores block comments)
            depth += countChar(stripped, '{')
            depth -= countChar(stripped, '}')
            if (depth < 0) depth = 0
        }

        return out.joinToString("\n")
    }

    private fun fixMissingClosingBraces(text: String, stats: FixStats): String {
        val open = countChar(text, '{')
        val close = countChar(text, '}')
        val missing = open - close
        if (missing <= 0) return text

        val sb = StringBuilder(text)
        if (!text.endsWith("\n")) sb.append("\n")
        repeat(missing) {
            sb.append("}")
            if (it < missing - 1) sb.append("\n")
        }
        stats.addedBraces += missing
        return sb.toString()
    }

    private fun stripBlockComments(line: String, inComment: Boolean): Pair<String, Boolean> {
        if (!inComment && !line.contains("/*")) return line to false
        var text = line
        var state = inComment

        while (true) {
            if (state) {
                val end = text.indexOf("*/")
                if (end == -1) return "" to true
                text = text.substring(end + 2)
                state = false
            } else {
                val start = text.indexOf("/*")
                if (start == -1) return text to false
                text = text.substring(0, start)
                state = true
            }
        }
    }

    private fun countChar(text: String, ch: Char): Int = text.count { it == ch }
}
