package rocketflow.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("type")
sealed class Trigger {

    @Serializable
    @SerialName("var_condition")
    data class VarCondition(
        val variable: String,
        val op: String,
        val value: Double
    ) : Trigger()
}
