package rocketflow

import java.io.File

class EnvParser {

    data class FormulaFile(
        val distortion: String,
        val formulas: List<String>
    )

    fun parse(f: File): FormulaFile {
        val lines = f.readLines().map { it.trim() }.filter { it.isNotEmpty() }

        if (lines.isEmpty())
            error("Formula file is empty: ${f.path}")

        // distortion=1
        val first = lines.first()
        if (!first.startsWith("distortion="))
            error("Formula file must start with 'distortion=': ${f.path}")

        val distortion = first.removePrefix("distortion=").trim()

        val formulas = lines.drop(1)

        return FormulaFile(distortion, formulas)
    }
}
