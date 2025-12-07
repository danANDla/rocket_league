import io.nats.client.*
import proto.TimeTick
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

fun main() {
    val nc = Nats.connect("nats://localhost:4222")

    var t = 0.0
    val dt = 0.1

    println("[TIME] Orchestrator started")

    while (true) {
        t += dt
        val msg = Json.encodeToString(TimeTick(t, dt))
        nc.publish("time.tick", msg.toByteArray())
        Thread.sleep((dt * 1000).toLong())
    }
}
