package com.rewiew.autofmt.util

object TwigAutoFixer {
    fun applyAll(text: String): String {
        var updated = text
        updated = fixPrintTagSpacing(updated)
        updated = fixAttributeNaming(updated)
        return updated
    }

    fun fixPrintTagSpacing(text: String): String {
        // Add space after '{{' unless whitespace control used
        var updated = text.replace(Regex("\\{\\{(?![\\s-])")) { "{{ " }
        // Add space before '}}' unless whitespace control used
        updated = updated.replace(Regex("(?<![\\s-])\\}\\}")) { " }}" }
        return updated
    }

    fun fixAttributeNaming(text: String): String {
        val regex = Regex("\\b(class|id)=(['\\\"])([^'\\\"]+)\\2", RegexOption.IGNORE_CASE)
        return regex.replace(text) { match ->
            val attrName = match.groupValues[1]
            val quote = match.groupValues[2]
            val value = match.groupValues[3]

            // Skip dynamic Twig expressions
            if (value.contains("{") || value.contains("}")) {
                return@replace match.value
            }

            val fixed = value.split(Regex("\\s+")).filter { it.isNotBlank() }.joinToString(" ") { token ->
                toKebabCase(token)
            }

            "$attrName=$quote$fixed$quote"
        }
    }

    private fun toKebabCase(input: String): String {
        var s = input.replace('_', '-')
        s = s.replace(Regex("([a-z0-9])([A-Z])"), "$1-$2")
        s = s.replace(Regex("([A-Z]+)([A-Z][a-z])"), "$1-$2")
        s = s.lowercase()
        s = s.replace(Regex("[^a-z0-9-]"), "-")
        s = s.replace(Regex("-+"), "-")
        s = s.trim('-')
        return s
    }
}
