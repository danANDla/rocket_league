package rocketflow

import kotlinx.serialization.Serializable

@Serializable
data class EnvModel(
    val distortion: String,
    val continuous_inputs: List<String>,
    val continuous_outputs: List<String>,
    val variable: Map<String, Double>,
    val formulas: List<String>
)
