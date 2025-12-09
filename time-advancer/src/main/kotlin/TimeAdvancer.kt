import io.nats.client.*
import proto.TimeTick
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

fun main() {
    val nc = Nats.connect("nats://localhost:4222")

    var t = 0.0
    val dt = 0.1
    val CPUclockFactor = 100
    var ticksBeforeClock = 0

    println("[TIME] Orchestrator started")

    while (true) {
        t += dt
        val msg = Json.encodeToString(TimeTick(t, dt))
        nc.publish("time.tick", msg.toByteArray())
        ticksBeforeClock += 1
        if(ticksBeforeClock == CPUclockFactor) {
            ticksBeforeClock = 0
            nc.publish("100ms", msg.toByteArray())
        }

        Thread.sleep((dt * 1000).toLong())

    }
}
