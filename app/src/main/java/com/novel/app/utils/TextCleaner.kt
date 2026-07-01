package com.novel.app.utils
import org.jsoup.Jsoup

object TextCleaner {
    fun clean(html: String): String {
        val doc = Jsoup.parse(html)
        doc.select("br").append("\\n")
        doc.select("p").append("\\n")
        var text = doc.text()
        text = text.replace("&nbsp;", " ").replace("&amp;", "&")
        text = text.replace("\\r".toRegex(), "")
        text = text.replace("[ \\t]+\\n".toRegex(), "\\n")
        text = text.replace("\\n{3,}".toRegex(), "\\n\\n")
        return text.trim()
    }

    fun extractTitleFromFirstLine(content: String): Pair<String?, String?>? {
        val lines = content.lines()
        for (i in lines.indices) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue
            val patterns = listOf(
                Regex("^(?:الفصل|Chapter|第)\\s*(\\d+)\\s*[：:.\\-]\\s*(.+)$", RegexOption.IGNORE_CASE),
                Regex("^(?:الفصل|Chapter|第)\\s*(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE),
                Regex("^(?:الفصل|Chapter|第)\\s*(\\d+)$", RegexOption.IGNORE_CASE)
            )
            for (pat in patterns) {
                val match = pat.find(line)
                if (match != null) {
                    val num = match.groupValues[1]
                    val title = match.groupValues.getOrNull(2)?.trim() ?: ""
                    val before = lines.subList(0, i).joinToString("\n")
                    val after = lines.subList(i + 1, lines.size).joinToString("\n")
                    val remaining = (before + "\n" + after).trim()
                    return if (title.isNotEmpty()) Pair(title, remaining) else Pair(num, remaining)
                }
            }
            break
        }
        return null
    }
}
