package reactive

import io.nats.client.Connection

class SrSystem {
    val inputs = SrInputs()
    val rules = mutableListOf<SrNode>()

    fun tick(nc: Connection) {
        for (rule in rules) {
            if (rule.evaluate(inputs)) {
                println("checking rule ${rule} is true")
                nc.publish(rule.trigger, ByteArray(0))
            }
        }
    }
}
