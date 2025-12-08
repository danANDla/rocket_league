package rocketflow.model

import kotlinx.serialization.Serializable

@Serializable
data class DEEvent(
    val name: String,
    val trigger: Trigger? = null,
    val effects: List<Effect>
)
