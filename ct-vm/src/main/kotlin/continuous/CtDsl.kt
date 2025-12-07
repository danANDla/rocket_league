package continuous

import continuous.lib.IntegratorNode
import continuous.lib.ConstantNode

class CtSystemBuilder {
    val system = CtSystem()

    fun integrator(
        name: String,
        block: IntegratorNode.() -> Unit
    ) {
        val node = IntegratorNode(name)
        node.block()
        system.addNode(node)
    }

    fun constant(
        name: String,
        block: ConstantNode.() -> Unit
    ) {
        val node = ConstantNode(name, 0.0)
        node.block()
        system.addNode(node)
    }

    fun connect(
        fromNode: String, fromOut: String,
        toNode: String, toOut: String,
    ) {
        system.connect(system.nodes[fromNode], fromOut, system.nodes[toNode], toOut)
    }
}

fun ctSystem(block: CtSystemBuilder.() -> Unit): CtSystem {
    val builder = CtSystemBuilder()
    builder.block()
    return builder.system
}