package rocketflow

import kotlinx.serialization.Serializable

@Serializable
data class CompileResult(
    val compiledFiles: List<String>,
    val status: String
)
