package rocketflow

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

// -------------------- Модели --------------------
@Serializable
data class EnvJson(
    val continuous_inputs: List<String> = emptyList(),
    val continuous_outputs: List<String> = emptyList(),
    val variable: Map<String, Double> = emptyMap(),
    val formulas: List<String> = emptyList()
)

// -------------------- Парсер --------------------
fun parseVariablesAndFormulas(file: File): Pair<Map<String, Double>, List<String>> {
    val variables = mutableMapOf<String, Double>()
    val formulas = mutableListOf<String>()

    val funcNames = setOf("sin", "cos", "tan", "abs", "sqrt", "log") // функции, которые не считаем переменными

    file.readLines()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .forEach { line ->

            // Присвоение числовой константы
            val assignMatch = Regex("""^([A-Za-z_]\w*)\s*=\s*([-+]?\d+(\.\d+)?)$""").matchEntire(line)
            if (assignMatch != null) {
                val (name, value) = assignMatch.destructured
                variables[name] = value.toDouble()
            } else {
                // Считаем формулой
                formulas.add(line)

                // Автоматически добавляем переменные, которые встречаются в формуле
                Regex("""[A-Za-z_]\w*""").findAll(line)
                    .map { it.value }
                    .filter { it !in funcNames } // исключаем функции
                    .forEach {
                        if (!variables.containsKey(it)) {
                            variables[it] = 0.0
                        }
                    }
            }
        }

    return variables to formulas
}

// -------------------- Генерация env.json --------------------
fun generateEnvJson(
    formulaFile: File,
    continuousInputs: List<String>,
    deEvents: List<DEEvent>
): EnvJson {
    val lines = formulaFile.readLines()

    // ключевые функции не считаем переменными
    val mathFuncs = setOf("sin", "cos", "abs", "tan")

    val variables = linkedMapOf<String, Double>()
    val formulas = mutableListOf<String>()

    val assignRegex = Regex("""^\s*([A-Za-z_]\w*)\s*=\s*(.+)$""")
    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

        val m = assignRegex.matchEntire(trimmed)
        if (m != null) {
            val name = m.groupValues[1]
            val expr = m.groupValues[2]
            // если правая часть это просто число — это переменная
            if (expr.toDoubleOrNull() != null) {
                variables[name] = expr.toDouble()
            } else {
                formulas.add(trimmed)
                // также инициализируем переменную, если её ещё нет
                variables.putIfAbsent(name, 0.0)
            }
        }
    }

    // continuous_outputs берём из эффектов типа AddExternal
    val continuousOutputs = deEvents
        .flatMap { it.effects }
        .filterIsInstance<Effect.AddExternal>()
        .map { it.target }
        .distinct()

    return EnvJson(
        continuous_inputs = continuousInputs,
        continuous_outputs = continuousOutputs,
        variable = variables,
        formulas = formulas
    )
}

