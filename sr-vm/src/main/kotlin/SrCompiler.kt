import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import reactive.ParsedCondition
import reactive.ComparisonOp
import reactive.SrSystem
import reactive.srSystem
import kotlinx.serialization.json.Json

@Serializable
data class SrConfig(
    @SerialName("sr_rules")
    val srRules: List<SrRule> = emptyList()
)

@Serializable
data class SrRule(
    val condition: String,
    val trigger: String
)

class SrCompiler {

    fun compile(config: SrConfig): SrSystem {
        return srSystem {
            config.srRules.forEach { r ->
                rule(r.condition, r.trigger)
            }
        }
    }

    fun compileFromJson(srConfigJson: String): SrSystem{
        val json = Json { ignoreUnknownKeys = true }
        return compile( json.decodeFromString<SrConfig>(srConfigJson))
    }
}
