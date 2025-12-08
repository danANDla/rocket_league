package rocketflow.model

import kotlinx.serialization.Serializable

@Serializable
data class SrRule(
    val condition: String,
    val trigger: String
)
