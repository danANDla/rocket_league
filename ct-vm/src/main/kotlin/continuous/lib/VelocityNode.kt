package continuous.lib

import continuous.CtNode

class VelocityNode(
    override val id: String,
    private val T: Double = 1.0
) : CtNode {

    override val inputs = mutableMapOf<String, Double>()
    override val outputs = mutableMapOf<String, Double>()
    override val state = mutableMapOf("V" to 0.0)

    override fun derivatives(): Map<String, Double> {
        val D = inputs["D"] ?: 0.0
        val V = state["V"] ?: 0.0

        // dV/dt = (D - V)/T
        val dV = (D - V) / T

        return mapOf("V" to dV)
    }

    override fun updateOutputs() {
        outputs["V"] = state["V"]!!
    }
}
