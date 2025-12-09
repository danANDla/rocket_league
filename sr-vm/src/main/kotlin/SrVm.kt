import SrCompiler
import io.nats.client.Nats
import proto.CtStateSnapshot
import reactive.SrSystem
import java.io.File
import kotlinx.serialization.json.Json

class SrVm() {

    private val compiler = SrCompiler()
    private val srSystem = compiler.compileFromJson(File("/home/danandla/botay/pes_kluch/rocket_league/de-vm/resources/compiled.json").readText())

    fun start() {
        val nc = Nats.connect("nats://localhost:4222")

        // 1. Подписаться на ct.state
        val ctDisp = nc.createDispatcher { msg ->
            val json = String(msg.data)
            val state = Json.decodeFromString<CtStateSnapshot>(json)

            // Обновить inputs
            state.nodes.forEach { (name, value) ->
                srSystem.inputs.update(name, value["out"]?:0.0)
            }
        }
        ctDisp.subscribe("ct.state")

        // 2. На каждый тик выполнить правила
        val tickDisp = nc.createDispatcher { msg ->
            srSystem.tick(nc)
        }
        tickDisp.subscribe("time.tick")
    }
}

fun main() {
    val sr = SrVm()

    sr.start()
}
