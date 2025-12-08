package rocketflow.runtime

import io.nats.client.Connection
import io.nats.client.Dispatcher
import io.nats.client.Nats
import kotlinx.serialization.json.Json
import rocketflow.model.ExtendedFullModel
import rocketflow.model.DEEvent
import rocketflow.model.Effect
import rocketflow.model.Trigger
import java.io.File
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * DeInterpreter
 *
 * Responsibilities:
 *  - load compiled.json (ExtendedFullModel)
 *  - initialize state from model.de.variables
 *  - subscribe to SR events via NATS (subject = event name)
 *  - subscribe to clock subject via NATS if model.clock defined;
 *      otherwise spawn local scheduler to generate ticks
 *  - on each tick:
 *      * execute tick-only events (events with trigger == null)
 *      * evaluate var_condition triggers and fire corresponding events (if condition met)
 *  - on receiving SR event via NATS: execute effects for that event
 *  - effects supported:
 *      - set_engine_power -> publish to "engine.<engineName>" subject with payload (JSON/plain)
 *      - update_variable -> change state variable by delta
 *
 * Logging: verbose println for every important step.
 */

class DeInterpreter(
    private val jsonPath: String = "compiled.json",
    private val natsUrl: String = "nats://localhost:4222"
) {
    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var model: ExtendedFullModel

    // state of DE variables
    private val state = mutableMapOf<String, Double>()

    // NATS connection + dispatcher
    private lateinit var nc: Connection
    private lateinit var dispatcher: Dispatcher

    // scheduler for local ticks if no clock subject provided
    private var scheduler: ScheduledExecutorService? = null

    // whether interpreter is running
    @Volatile
    private var running = false

    // mutex for state updates (simple)
    private val stateLock = Any()

    fun load() {
        val text = File(jsonPath).readText()
        model = json.decodeFromString(ExtendedFullModel.serializer(), text)

        // initialize state
        state.clear()
        state.putAll(model.de.variables)

        println("DE Interpreter: loaded ${model.de.events.size} events, ${model.de.engines.size} engines.")
        println("Initial DE variables: $state")
    }

    fun run() {
        if (!::model.isInitialized) {
            error("Model not loaded. Call load() before start().")
        }

        println("Connecting to NATS at $natsUrl ...")
        nc = Nats.connect(natsUrl)

        dispatcher = nc.createDispatcher { msg ->
            // handler for incoming subjects; we handle subscriptions below explicitly
            val subj = msg.subject
            // We treat the subject as the event name
            handleIncomingSrEvent(subj)
        }

        // subscribe to SR events
        for (ev in model.all_sr_events) {
            dispatcher.subscribe(ev)
            println("Subscribed to SR event subject: '$ev'")
        }

        // subscribe to clock subject if present, otherwise create a local scheduler
        val clockSubject = model.clock?.takeIf { it.isNotBlank() }
        if (clockSubject != null) {
            dispatcher.subscribe(clockSubject)
            println("Subscribed to clock subject: '$clockSubject' (will perform ticks on incoming messages)")
        } else {
            // no external clock — create a local scheduler that will call onTick at fixed rate
            // default tick period 100ms
            val periodMs = 100L
            scheduler = Executors.newSingleThreadScheduledExecutor()
            scheduler!!.scheduleAtFixedRate({
                try {
                    onTick()
                } catch (ex: Exception) {
                    println("Error during onTick: ${ex.message}")
                    ex.printStackTrace()
                }
            }, 0, periodMs, TimeUnit.MILLISECONDS)
            println("No clock subject specified; started local tick scheduler with period ${periodMs}ms")
        }

        // also subscribe to specific internal event subjects if any DE events should be externally receivable
        // (we already subscribed SR events and clock; DE internal triggers are handled onTick)

        // set running flag
        running = true

        println("DE Interpreter is RUNNING. Waiting for messages and ticks...")
        // spin thread to keep process alive (NATS dispatcher runs callbacks on its own threads)
        thread(start = true, isDaemon = false) {
            while (running) {
                Thread.sleep(1000)
            }
        }
    }

    private fun handleIncomingSrEvent(eventName: String) {
        println("[EVENT:SR] Received event from SR: '$eventName' at ${Instant.now()}")
        // find event definition in model.de.events by name
        val evt = model.de.events.find { it.name == eventName }
        if (evt != null) {
            // execute effects of this event
            executeEventEffects(evt, source = "SR")
        } else {
            // if no DE-event defined with this name, it may still be just a SR-notification; log it
            println(" → No DE-event definition for '$eventName' in model.de.events; ignoring or user-defined handling may be missing.")
            onTick()
        }
    }

    /**
     * Called on every tick.
     * Order:
     *   1) execute tick-only events (events with trigger == null)
     *   2) evaluate var_condition triggers (trigger.type == var_condition) and execute those whose condition is met
     */
    private fun onTick() {
        val tickTime = Instant.now()
        println("[TICK] $tickTime")

        // 1) tick-only events (trigger == null) — run them
        val tickOnly = model.de.events.filter { it.trigger == null }
        if (tickOnly.isNotEmpty()) {
            println(" → Executing ${tickOnly.size} tick-only events")
        }
        for (evt in tickOnly) {
            executeEventEffects(evt, source = "TICK")
        }

        // 2) evaluate var_condition triggers
        val condEvents = model.de.events.filter { it.trigger is Trigger.VarCondition }
        for (evt in condEvents) {
            val trig = evt.trigger as Trigger.VarCondition
            val conditionSatisfied = evaluateVarCondition(trig.variable, trig.op, trig.value)
            if (conditionSatisfied) {
                println("[EVENT:COND] Triggered '${evt.name}' because ${trig.variable} ${trig.op} ${trig.value} is true")
                executeEventEffects(evt, source = "COND")
            }
        }
    }

    /**
     * Evaluate a simple variable condition: op in { "<", ">", "<=", ">=", "==", "!=" }
     */
    private fun evaluateVarCondition(variable: String, op: String, value: Double): Boolean {
        val current = synchronized(stateLock) { state[variable] }
        if (current == null) {
            println(" → Variable '$variable' not present in state; treating condition as false")
            return false
        }
        return when (op) {
            "<"  -> current < value
            ">"  -> current > value
            "<=" -> current <= value
            ">=" -> current >= value
            "==" -> current == value
            "!=" -> current != value
            else -> {
                println(" → Unknown operator '$op' in trigger; treating as false")
                false
            }
        }
    }

    /**
     * Apply the effects listed on an event.
     * Supported effect types:
     *  - set_engine_power: publishes to engine.<engineName> subject with JSON/plain payload
     *  - update_variable: modifies state[variable] += delta
     *
     * source indicates why the event fired: "SR", "TICK", "COND", etc.
     */
    private fun executeEventEffects(evt: DEEvent, source: String) {
        println(" → Executing event '${evt.name}' (source=$source), effects=${evt.effects.size}")
        for ((idx, eff) in evt.effects.withIndex()) {
            when (eff) {
                is Effect.SetEnginePower -> {
                    val engine = eff.engine
                    val power = eff.power
                    // publish engine command to NATS subject "engine.<engineName>"
                    val subj = "engine.$engine"
                    val payload = """{"cmd":"set_power","engine":"$engine","power":$power}"""
                    nc.publish(subj, payload.toByteArray())
                    println("    [Effect ${idx+1}] set_engine_power -> $engine = $power (published to '$subj')")
                }

                is Effect.UpdateVariable -> {
                    val varName = eff.variable
                    val delta = eff.delta
                    val old = synchronized(stateLock) { state.getOrDefault(varName, 0.0) }
                    val new = old + delta
                    synchronized(stateLock) { state[varName] = new }
                    println("    [Effect ${idx+1}] update_variable -> $varName: $old -> $new (delta=$delta)")
                }

                else -> {
                    println("    [Effect ${idx+1}] Unknown effect type: $eff (ignored)")
                }
            }
        }
    }

    /**
     * Stop interpreter: shutdown scheduler and NATS connection.
     */
    fun stop() {
        println("Stopping DE Interpreter...")
        running = false
        scheduler?.shutdownNow()
        try {
            dispatcher.unsubscribe("*")
        } catch (_: Exception) {}
        try {
            nc.close()
        } catch (_: Exception) {}
        println("DE Interpreter stopped.")
    }
}
