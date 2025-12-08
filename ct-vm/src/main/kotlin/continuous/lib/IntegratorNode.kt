package continuous.lib
import continuous.CtNode

class IntegratorNode(
    override val id: String,
    override var isExternal: Boolean = true,
    var initialState: Double = 0.0,
    var derivativeFunc: (IntegratorNode) -> Double = { 0.0 }
) : CtNode {

    override val inputs = mutableMapOf<String, Double>()
    override val outputs = mutableMapOf("out" to initialState)
    override val state = mutableMapOf("state" to initialState)

    override fun derivatives(): Map<String, Double> =
        mapOf("state" to derivativeFunc(this))

    override fun updateOutputs() {
        outputs["out"] = state["state"] ?: initialState
    }
}