import continuous.CtSystem
import continuous.ctSystem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


@Serializable
data class ContinuousModelConfig(
    val continuous_inputs: List<String>,
    val de: DESection
)

@Serializable
data class DESection(
    val engines: List<String>,
)

class ContinuousTimeCompiler {
    fun compile(config: ContinuousModelConfig): CtSystem {
        return ctSystem {
            config.continuous_inputs.forEach { name ->
                externalInput(name) {
                    val inputId = name.split(".")
                    topic = inputId[0]
                    component = inputId[1].lowercase()
                }
            }

            config.de.engines.forEach { name ->
                engineCommand("command$name") {
                    topic = "engine.$name"
                    component = "power"
                }

                integrator(name) {
                    initialState = 0.0
                    derivativeFunc = { node ->
                        val D = node.inputs["desired"] ?: 0.0
                        val V = node.state["state"] ?: 0.0
                        (D - V) / 0.5
                    }
                    isExternal = true
                }

                connect("command$name", "out", name, "desired")
            }

        }
    }

    fun compileFromJson(ctConfigJson: String): CtSystem{
        val json = Json { ignoreUnknownKeys = true }
        return compile( json.decodeFromString<ContinuousModelConfig>(ctConfigJson))
    }
}