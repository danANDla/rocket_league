package rocketflow

import java.io.File

/**
 * Простая реализация парсера SR DSL.
 *
 * Формат строк:
 *   rule <condition> -> <EVENT>
 *
 * Пример:
 *   rule X > 100 and Angle < 30 -> HIGH_ANGLE
 *
 * Мы сохраняем условие как строку (без дальнейшей проверки).
 */

class SrParser {
    fun parseFile(f: File): List<SrRule> {
        val out = mutableListOf<SrRule>()
        val lines = f.readLines()
        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            if (!line.startsWith("rule ")) continue
            val body = line.removePrefix("rule").trim()
            // разделяем по '->'
            val parts = body.split("->")
            if (parts.size != 2) continue // пропускаем некорректные — минимальная валидация
            val cond = parts[0].trim()
            val ev = parts[1].trim()
            out.add(SrRule(condition = cond, trigger = ev))
        }
        return out
    }

    fun parseSREvents(lines: List<String>): List<String> =
    lines
        .filter { it.trim().startsWith("rule ") }
        .map { line ->
            val parts = line.split("->")
            parts[1].trim()          // правый блок — это событие
        }


    fun findContinuousInputs(srRules: List<SrRule>): List<String> {
        // Простая эвристика: извлечём имена переменных из условий по regex [A-Za-z_]\w*
        // и вернём уникальные имена, кроме ключевых слов (and/or/not)
        val keywords = setOf("and","or","not")
        val re = Regex("""[A-Za-z_]\w*""")
        val set = linkedSetOf<String>()
        for (r in srRules) {
            val tokens = re.findAll(r.condition).map { it.value }.filter { it.lowercase() !in keywords }
            tokens.forEach { set.add(it) }
        }
        return set.toList()
    }
}
