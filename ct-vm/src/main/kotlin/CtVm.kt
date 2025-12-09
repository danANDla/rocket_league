import continuous.CtSystem
import continuous.ctSystem
import continuous.lib.EngineCommandNode
import continuous.lib.ExternalInputNode
import proto.CtStateSnapshot
import proto.TimeTick
import io.nats.client.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import proto.CtExternalUpdate
import proto.EnginesSnapshot
import proto.MsgCommand
import proto.Vector
import java.io.File

const val T = 1.5

class CtVm {

    private val compiler = ContinuousTimeCompiler()
    private val ctSystem = compiler.compileFromJson(File("/home/danandla/botay/pes_kluch/rocket_league/de-vm/resources/compiled.json").readText())

    fun start() {
        val nc = Nats.connect("nats://localhost:4222")
        val d = nc.createDispatcher({ msg: Message ->
            val tick = Json.decodeFromString<TimeTick>(String(msg.data))

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

        ctSystem.nodes.values.filterIsInstance<EngineCommandNode>().forEach { commandNode ->
            println("Subscribing external input ${commandNode.id} to ${commandNode.topic}")
            commandNode.dispatcher = nc.createDispatcher { msg ->
                val valueUpdate = Json.decodeFromString<MsgCommand>(String(msg.data))
                commandNode.value = valueUpdate
            }
            commandNode.dispatcher.subscribe(commandNode.topic)
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
        val externalState = mutableMapOf<String, Double>()
        for ((keynode, n) in ctSystem.nodes) {
            if(n.isExternal) {
                for((keyout, out) in n.outputs) {
                    externalState[keynode] = out
                }
            }
        }
        nc.publish("engines", Json.encodeToString<MutableMap<String, Double>>(externalState).toByteArray())
    }
}

fun main() {
    val ct = CtVm()

    ct.start()
}