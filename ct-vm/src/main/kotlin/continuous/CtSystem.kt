package continuous

class CtSystem {
    val nodes = mutableMapOf<String, CtNode>()
    private val connections = mutableListOf<CtConnection>()

    fun addNode(node: CtNode) { nodes[node.id] = node }

    fun connect(from: CtNode?, out: String, to: CtNode?, input: String) {
        if(from != null && to != null)
            connections += CtConnection(from, out, to, input)
        else
            println("Empty connections $out to $input")
    }

    fun step(dt: Double) {
        // 1. собрать значения всех выходов в входы
        propagateSignals()

        // 2. продвинуть каждый узел по времени (интеграция)
        for ((key, n) in nodes) n.integrate(dt)

        // 3. обновить выходы (после интеграции)
        for ((key, n) in nodes) n.updateOutputs()
    }

    private fun propagateSignals() {
        for (c in connections) {
            val v = c.fromNode.outputs[c.fromPort]
            if (v != null)
                c.toNode.inputs[c.toPort] = v
        }
    }
}
