package rocketflow

import kotlinx.serialization.*
import kotlinx.serialization.json.Json

@Serializable
data class SrRule(val condition: String, val trigger: String)

@Serializable
data class ExtendedFullModel(
    val all_sr_events: List<String>,
    val continuous_inputs: List<String>,
    val clock: String? = null,
    val sr_rules: List<SrRule>,
    val de: DEModel
)

@Serializable
data class DEModel(
    val variables: Map<String, Double> = emptyMap(),
    val events: List<DEEvent> = emptyList(),
    val engines: List<String> = emptyList()
)

@Serializable
data class DEEvent(
    val name: String,
    val trigger: Trigger? = null,
    val effects: List<Effect> = emptyList()
)

@Serializable
sealed class Trigger {
    @Serializable
    @SerialName("var_condition")
    data class VarCondition(val variable: String, val op: String, val value: Double) : Trigger()

    @Serializable
    @SerialName("expr_trigger")
    data class ExprTrigger(val expr: Expr) : Trigger()
}

@Serializable
sealed class Effect {
    @Serializable
    @SerialName("set_engine_power")
    data class SetEnginePower(val engine: String, val power: Double) : Effect()

    @Serializable
    @SerialName("update_variable")
    data class UpdateVariable(val variable: String, val delta: Expr) : Effect()

    @Serializable
    @SerialName("add_external")
    data class AddExternal(val target: String, val delta: Expr? = null) : Effect()
}

@Serializable
sealed class Expr {
    @Serializable
    @SerialName("const")
    data class Const(val value: Double) : Expr()

    @Serializable
    @SerialName("variable")
    data class Variable(val name: String) : Expr()

    @Serializable
    @SerialName("unary_op")
    data class UnaryOp(val op: String, val arg: Expr) : Expr()

    @Serializable
    @SerialName("binary_op")
    data class BinaryOp(val left: Expr, val op: String, val right: Expr) : Expr()
}
