import continuous.CtSystem
import continuous.ctSystem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ContinuousModelConfig(
    @SerialName("continuous_inputs")
    val continuousInputs: List<String> = emptyList(),

    val clock: String,

    @SerialName("sr_rules")
    val srRules: List<SrRule> = emptyList()
)

@Serializable
data class SrRule(
    val condition: String,
    val event: String
)

class ContinuousTimeCompiler {
    fun compile(config: ContinuousModelConfig): CtSystem {
        return ctSystem {
            config.continuousInputs.forEach { name ->
                externalInput(name) {
                    val inputId = name.split(".")
                    topic = inputId[0]
                    component = inputId[1].lowercase()
                    isExternal = true
                }
            }
        }
    }

    fun compileFromJson(ctConfigJson: String): CtSystem{
        return compile( Json.decodeFromString<ContinuousModelConfig>(ctConfigJson))
    }
}