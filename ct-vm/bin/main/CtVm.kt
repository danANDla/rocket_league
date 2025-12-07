import simulation.*
import kotlin.math.E
import io.nats.client.*
import proto.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString


fun staticSimulation() {
    val generator = simulation.Generator(
        D = 100.0,
        V = 0.0,
        T = 1 - 1 / E
    )

    // val scheduler = Scheduler(generator, dt = 0.01)
    // scheduler.runUntil(5.0)
    // println("Result V at t = 5s = ${generator.V}")

    val dt = 0.01
    val scheduler = Scheduler(generator, dt)

    val times = mutableListOf<Double>()
    val Ds = mutableListOf<Double>()
    val Vs = mutableListOf<Double>()

    while (scheduler.currentTime < 5.0) {
        scheduler.runUntil(scheduler.currentTime + dt)

        times.add(scheduler.currentTime)
        Ds.add(generator.D)
        Vs.add(generator.V)
    }
    plotSimulation(times, Ds, Vs)
}
const val T = 1.5

fun main() {
    val nc = Nats.connect("nats://nats:4222")
    val d = nc.createDispatcher({ msg -> 
        println(" msg.data received")
    })

    var V = 0.0
    var D = 100.0

    d.subscribe("time.tick")
    // nc.subscribe("time.tick") { msg: Message ->
    //     val tick = Json.decodeFromString<TimeTick>(String(msg.data))
    //     val dt = tick.dt

    //     val dVdt = (D - V) / T
    //     V += dVdt * dt

    //     val out = Json.encodeToString(CtState(V))
    //     nc.publish("ct.out", out.toByteArray())
    // }

    Thread.currentThread().join()
}