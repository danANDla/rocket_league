package continuous.lib

import continuous.CtNode

class ConstantNode (
    override val id: String,
    var value: Double
): CtNode{
    override val inputs =  mutableMapOf<String, Double>()
    override val outputs =  mutableMapOf<String, Double>("out" to value)
    override val state = mutableMapOf<String, Double>("out" to value)

    override fun derivatives() = emptyMap<String, Double>()
    override fun integrate(dt: Double) { }
    override fun updateOutputs() { }
}