package reactive

fun parse(str: String): ParsedCondition {
    val tokens = str.trim().split(" ")
    require(tokens.size == 3) { "Bad condition syntax: $str" }

    val variable = tokens[0]
    val op = parseOp(tokens[1])
    val value = tokens[2].toDouble()

    return ParsedCondition(variable, op, value)
}

private fun parseOp(op: String): ComparisonOp =
    when (op) {
        ">"  -> ComparisonOp.GT
        "<"  -> ComparisonOp.LT
        "==" -> ComparisonOp.EQ
        ">=" -> ComparisonOp.GE
        "<=" -> ComparisonOp.LE
        else -> error("Unknown operator: $op")
    }

class SrSystemBuilder {
    val system = SrSystem()

    fun rule(condition: String, trigger: String) {
        val parsed = parse(condition)
        val node = SrNode(parsed, trigger)
        system.rules += node
    }
}

fun srSystem(block: SrSystemBuilder.() -> Unit): SrSystem {
    val b = SrSystemBuilder()
    b.block()
    return b.system
}
