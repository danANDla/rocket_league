import continuous.CtSystem
import continuous.ctSystem
import continuous.lib.ExternalInputNode
import proto.CtStateSnapshot
import proto.TimeTick
import io.nats.client.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import proto.CtExternalUpdate
import proto.Vector

const val T = 1.5

class CtVm {

    private val ctSystem = ctSystem {
        constant("desired") {
            value = 100.0
            isExternal = false
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

        integrator("sensorX") {
            initialState = 0.0
            derivativeFunc = { node ->
                val D = node.inputs["realworld"] ?: 0.0
                val V = node.state["state"] ?: 0.0
                D - V
            }
            isExternal = false
        }

        externalInput("coordinates.x") {
            topic = "coordinates"
            component = "x"
            isExternal = true
        }

        externalInput("coordinates.y") {
            topic = "coordinates"
            component = "y"
        }

        // connect("engine", "out", "engine", "desired")
        connect("coordinates.x", "out", "sensorX", "realworld")
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

        ctSystem.nodes.values.filterIsInstance<ExternalInputNode>().forEach { extNode ->
            println("Subscribing external input ${extNode.id} to ${extNode.topic}")
            extNode.dispatcher = nc.createDispatcher { msg ->
                val valueUpdate = Json.decodeFromString<Vector>(String(msg.data))
                extNode.value = valueUpdate
            }
            extNode.dispatcher.subscribe(extNode.topic)
        }

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