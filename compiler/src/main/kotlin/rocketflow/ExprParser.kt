package rocketflow

object ExprParser {

    fun parse(input: String): Expr {
        val trimmed = input.trim()
        // Try binary ops in order of precedence: + - * /
        for (op in listOf("+", "-", "*", "/")) {
            val parts = splitTopLevel(trimmed, op)
            if (parts != null) {
                return Expr.BinaryOp(parse(parts.first), op, parse(parts.second))
            }
        }
        // Unary functions
        val unaryRegex = Regex("""(\w+)\((.+)\)""")
        val m = unaryRegex.matchEntire(trimmed)
        if (m != null) {
            val fn = m.groupValues[1]
            val arg = parse(m.groupValues[2])
            return Expr.UnaryOp(fn, arg)
        }
        // Constant
        trimmed.toDoubleOrNull()?.let { return Expr.Const(it) }
        // Variable
        return Expr.Variable(trimmed)
    }

    private fun splitTopLevel(expr: String, op: String): Pair<String, String>? {
        var depth = 0
        for (i in expr.indices.reversed()) {
            val c = expr[i]
            if (c == ')') depth++
            if (c == '(') depth--
            if (depth == 0 && expr[i] == op[0]) {
                val left = expr.substring(0, i)
                val right = expr.substring(i + 1)
                return left to right
            }
        }
        return null
    }
}
