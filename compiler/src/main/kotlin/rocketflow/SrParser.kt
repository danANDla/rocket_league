package rocketflow

import java.io.File

class SrParser {

    fun parseFile(f: File): List<SrRule> {
        val out = mutableListOf<SrRule>()
        f.readLines().forEach { lineRaw ->
            val line = lineRaw.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEach
            if (!line.startsWith("rule")) return@forEach

            val parts = line.removePrefix("rule").split("->").map { it.trim() }
            if (parts.size == 2) out.add(SrRule(parts[0], parts[1]))
        }
        return out
    }

    fun findContinuousInputs(srRules: List<SrRule>): List<String> {
        val keywords = setOf("and", "or", "not")
        val re = Regex("""[A-Za-z_]\w*""")
        val set = linkedSetOf<String>()
        srRules.forEach { rule ->
            val tokens = re.findAll(rule.condition).map { it.value }.filter { it.lowercase() !in keywords }
            tokens.forEach { set.add(it) }
        }
        return set.toList()
    }
}
