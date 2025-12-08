import continuous.CtSystem
import continuous.ctSystem
import proto.CtStateSnapshot
import proto.TimeTick
import io.nats.client.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import proto.CtExternalUpdate

const val T = 1.5

class CtVm {

    private val ctSystem = ctSystem {
        constant("desired") {
            value = 100.0
        }

        integrator("engine") {
            initialState = 0.0
            derivativeFunc = { node ->
                val D = node.inputs["desired"] ?: 0.0
                val V = node.state["state"] ?: 0.0
                (D - V) / 0.5
            }
            isExternal = true
        }

        connect("desired", "out", "engine", "desired")
    }

    fun start() {
        val nc = Nats.connect("nats://localhost:4222")

        val d = nc.createDispatcher({ msg: Message ->
            println(" msg.data received")
            val tick = Json.decodeFromString<TimeTick>(String(msg.data))
            println("tick dt: ${tick.dt}; tick t: ${tick.t};")

            ctSystem.step(tick.dt)

            val snapshot = collectState()
            nc.publish("ct.state", Json.encodeToString<CtStateSnapshot>(snapshot).toByteArray())

            publishExternals(nc)
        })

        d.subscribe("time.tick")
    }

    private fun collectState(): CtStateSnapshot {
        val map = mutableMapOf<String, Map<String, Double>>()
        for ((key, n) in ctSystem.nodes) {
            map[n.id] = n.outputs
        }
        return CtStateSnapshot(map)
    }

    private fun publishExternals(nc: Connection) {
        for ((keynode, n) in ctSystem.nodes) {
            if(n.isExternal) {
                for((keyout, out) in n.outputs) {
                    val update = CtExternalUpdate(keynode, out)
                    nc.publish("external.rocket", Json.encodeToString<CtExternalUpdate>(update).toByteArray())
                }
            }
        }
    }
}

fun main() {
    val ct = CtVm()

    ct.start()
}