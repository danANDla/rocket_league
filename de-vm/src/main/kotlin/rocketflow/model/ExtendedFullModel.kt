package rocketflow.model

import kotlinx.serialization.Serializable

@Serializable
data class ExtendedFullModel(
    val all_sr_events: List<String>,
    val clock: String? = null,
    val de: DESection
)

@Serializable
data class DESection(
    val variables: Map<String, Double>,
    val engines: List<String>,
    val events: List<DEEvent>,
)