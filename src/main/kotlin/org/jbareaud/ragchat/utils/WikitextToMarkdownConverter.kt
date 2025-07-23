package org.jbareaud.ragchat.utils

private const val PAGENAME_MARKER = "{{PAGENAME}}"

fun convertWikitextToMarkdown(input: String, title: String): String {
    var output = input

    // Replace {{PAGENAME}} marker
    output = output.replace(PAGENAME_MARKER, title)

    // Convert Infobox card and LoreInfobox to bulleted lists
    output = convertInfoboxes(output)

    // Section headers
    output = output.replace(Regex("^======\\s*(.*?)\\s*======\\s*$", RegexOption.MULTILINE), "###### $1")
    output = output.replace(Regex("^=====\\s*(.*?)\\s*=====$", RegexOption.MULTILINE), "##### $1")
    output = output.replace(Regex("^====\\s*(.*?)\\s*====$", RegexOption.MULTILINE), "#### $1")
    output = output.replace(Regex("^===\\s*(.*?)\\s*===$", RegexOption.MULTILINE), "### $1")
    output = output.replace(Regex("^==\\s*(.*?)\\s*==$", RegexOption.MULTILINE), "## $1")
    output = output.replace(Regex("^=\\s*(.*?)\\s*=$", RegexOption.MULTILINE), "# $1")

    // Bold and italic
    output = output.replace("'''''(.*?)'''''".toRegex(), "***$1***")
    output = output.replace("'''(.*?)'''".toRegex(), "**$1**")
    output = output.replace("''(.*?)''".toRegex(), "*$1*")

    // Internal links
    output = output.replace(Regex("\\[\\[([^\\]|]+)\\|([^\\]]+)\\]\\]"), "$2")
    output = output.replace(Regex("\\[\\[([^\\]]+)\\]\\]"), "$1")

    // Convert {{quote|...}} to italicized text
    output = output.replace(Regex("""\{\{quote\|([^}]+)}}"""), "_$1_")

    // Convert {{Quote|...|...}} (with optional second arg) to italicized text
    output = output.replace(Regex("""\{\{Quote\|([^|}]+)(\|[^}]+)?}}"""), "_$1_")

    // Convert {{Lore|...}} to inline code
    output = output.replace(Regex("""\{\{Lore\|([^}|]+)(\|[^}]+)?}}""")) {
        val aspect = it.groupValues[1]
        "`$aspect`"
    }

    // Convert {{lore|...}} to inline code (case-insensitive)
    output = output.replace(Regex("""\{\{lore\|([^}|]+)(\|[^}]+)?}}""", RegexOption.IGNORE_CASE)) {
        val aspect = it.groupValues[1]
        "`$aspect`"
    }

    // Convert [[Link]] to [Link](Link)
    output = output.replace(Regex("""\[\[([^\]|]+)\]\]""")) {
        val link = it.groupValues[1]
        "[$link]($link)"
    }

    // Convert [[Link|Text]] to [Text](Link)
    output = output.replace(Regex("""\[\[([^\]|]+)\|([^\]]+)\]\]""")) {
        val link = it.groupValues[1]
        val text = it.groupValues[2]
        "[$text]($link)"
    }

    // Internal links
    output = output.replace(Regex("\\[\\[([^\\]|]+)\\|([^\\]]+)\\]\\]"), "$2")
    output = output.replace(Regex("\\[\\[([^\\]]+)\\]\\]"), "$1")

    // HTML Entities
    output = output
        .replace("&nbsp;", " ")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&amp;", "&")


    // Clean up remaining templates (like {{PAGENAME}} etc.)
    output = output.replace(Regex("""\{\{[^{}]+}}"""), "") // remove unhandled templates
    output = output.replace("__NOTOC__", "")
    output = output.replace("<nowiki>", "")
        .replace("<?nowiki>", "")

    return output.trim()
}

private fun convertInfoboxes(text: String): String {
    val result = StringBuilder()
    var i = 0
    while (i < text.length) {
        if (text.startsWith("{{Infobox card", i) || text.startsWith("{{LoreInfobox", i)) {
            val start = i
            var braceCount = 2
            i += 2 // Skip the opening {{
            while (i < text.length && braceCount > 0) {
                if (text.startsWith("{{", i)) {
                    braceCount += 2
                    i += 2
                } else if (text.startsWith("}}", i)) {
                    braceCount -= 2
                    i += 2
                } else {
                    i++
                }
            }
            val block = text.substring(start, i)
            result.append("Information:\n")
            result.append(parseInfoboxToMarkdownTable(block)).append("\n")
        } else {
            result.append(text[i])
            i++
        }
    }
    return result.toString()
}

private fun parseInfoboxToMarkdownTable(block: String): String {
    val lines = block
        .removePrefix("{{Infobox card")
        .removePrefix("{{LoreInfobox")
        .removeSuffix("}}")
        .trim()
        .lines()
        .mapNotNull {
            val line = it.trim()
            if (line.startsWith("|")) {
                val parts = line.removePrefix("|").split("=", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    val value = parts[1].trim()
                    key to value
                } else null
            } else null
        }

    if (lines.isEmpty()) return ""

    val builder = StringBuilder()
    for ((key, value) in lines) {
        builder.append("| $key | $value |\n")
    }
    return builder.toString()
}