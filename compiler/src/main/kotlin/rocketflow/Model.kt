package rocketflow

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExtendedFullModel(
    val all_sr_events: List<String>,
    val continuous_inputs: List<String>,
    val clock: String?,
    val sr_rules: List<SrRule>,
    val de: DEModel
)

@Serializable
data class SrRule(
    val condition: String,
    val trigger: String
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
}

@Serializable
sealed class Effect {
    @Serializable
    @SerialName("set_engine_power")
    data class SetEnginePower(val engine: String, val power: Double) : Effect()

    @Serializable
    @SerialName("update_variable")
    data class UpdateVariable(val variable: String, val delta: Double) : Effect()
}
