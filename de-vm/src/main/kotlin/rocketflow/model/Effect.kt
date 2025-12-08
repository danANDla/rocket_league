package rocketflow.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("type")
sealed class Effect {

    @Serializable
    @SerialName("set_engine_power")
    data class SetEnginePower(
        val engine: String,
        val power: Double
    ) : Effect()

    @Serializable
    @SerialName("update_variable")
    data class UpdateVariable(
        val variable: String,
        val delta: Double
    ) : Effect()
}
