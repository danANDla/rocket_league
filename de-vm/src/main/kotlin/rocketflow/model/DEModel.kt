package rocketflow.model

import kotlinx.serialization.Serializable

@Serializable
data class DEModel(
    val variables: Map<String, Double>,
    val events: List<DEEvent>,
    val engines: List<String>
)
