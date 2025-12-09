package reactive


data class ParsedCondition(
    val variable: String,
    val op: ComparisonOp,
    val value: Double
)

enum class ComparisonOp {
    GT, LT, EQ, GE, LE
}

data class SrInputs(
    private val values: MutableMap<String, Double> = mutableMapOf()
) {
    fun update(name: String, value: Double) {
        values[name] = value
    }
    operator fun get(name: String): Double? = values[name]
}

class SrNode(
    val condition: ParsedCondition,
    val trigger: String
) {
    fun evaluate(inputs: SrInputs): Boolean {
        val v = inputs[condition.variable] ?: return false
        return when (condition.op) {
            ComparisonOp.GT -> v > condition.value
            ComparisonOp.LT -> v < condition.value
            ComparisonOp.EQ -> v == condition.value
            ComparisonOp.GE -> v >= condition.value
            ComparisonOp.LE -> v <= condition.value
        }
    }
}
