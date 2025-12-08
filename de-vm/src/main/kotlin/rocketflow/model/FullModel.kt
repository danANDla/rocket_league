package rocketflow.model

import kotlinx.serialization.Serializable

@Serializable
data class FullModel(
    val continuous_inputs: List<String>,
    val clock: String?,
    val sr_rules: List<SrRule>,
    val de: DEModel
)
