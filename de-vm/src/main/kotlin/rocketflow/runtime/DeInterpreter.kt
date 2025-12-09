package rocketflow.runtime

import io.nats.client.Connection
import io.nats.client.Dispatcher
import io.nats.client.Nats
import kotlinx.serialization.json.Json
import rocketflow.model.*
import java.io.File
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.math.*

// --- New: simple expression evaluator for DE formulas ---
object EvalExpr {
    fun eval(expr: String, vars: Map<String, Double>): Double {
        val e = expr.replace("\\s+".toRegex(), "")
        return evalRecursive(e, vars)
    }

    private fun evalRecursive(expr: String, vars: Map<String, Double>): Double {
        var s = expr
        // functions
        if (s.startsWith("sin(") && s.endsWith(")")) {
            val inner = s.substring(4, s.length - 1)
            return sin(evalRecursive(inner, vars))
        }
        if (s.startsWith("cos(") && s.endsWith(")")) {
            val inner = s.substring(4, s.length - 1)
            return cos(evalRecursive(inner, vars))
        }
        if (s.startsWith("abs(") && s.endsWith(")")) {
            val inner = s.substring(4, s.length - 1)
            return abs(evalRecursive(inner, vars))
        }
        // parentheses
        if (s.startsWith("(") && s.endsWith(")")) {
            return evalRecursive(s.substring(1, s.length - 1), vars)
        }
        // binary operators
        for (op in listOf('+', '-')) {
            val idx = s.lastIndexOf(op)
            if (idx > 0) {
                val left = s.substring(0, idx)
                val right = s.substring(idx + 1)
                return if (op == '+') evalRecursive(left, vars) + evalRecursive(right, vars)
                else evalRecursive(left, vars) - evalRecursive(right, vars)
            }
        }
        for (op in listOf('*', '/')) {
            val idx = s.lastIndexOf(op)
            if (idx > 0) {
                val left = s.substring(0, idx)
                val right = s.substring(idx + 1)
                return if (op == '*') evalRecursive(left, vars) * evalRecursive(right, vars)
                else evalRecursive(left, vars) / evalRecursive(right, vars)
            }
        }
        // number or variable
        return s.toDoubleOrNull() ?: vars[s] ?: 0.0
    }
}

class DeInterpreter(
    private val jsonPath: String = "compiled.json",
    private val natsUrl: String = "nats://localhost:4222"
) {
    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var model: ExtendedFullModel
    private val state = mutableMapOf<String, Double>()
    private lateinit var nc: Connection
    private lateinit var dispatcher: Dispatcher
    private var scheduler: ScheduledExecutorService? = null
    @Volatile private var running = false
    private val stateLock = Any()

    fun load() {
        val text = File(jsonPath).readText()
        model = json.decodeFromString(ExtendedFullModel.serializer(), text)
        state.clear()
        state.putAll(model.de.variables)
        println("DE Interpreter: loaded ${model.de.events.size} events, ${model.de.engines.size} engines.")
        println("Initial DE variables: $state")
    }

    fun run() {
        if (!::model.isInitialized) error("Model not loaded. Call load() first.")
        println("Connecting to NATS at $natsUrl ...")
        nc = Nats.connect(natsUrl)
        dispatcher = nc.createDispatcher { msg -> handleIncomingSrEvent(msg.subject) }
        for (ev in model.all_sr_events) {
            dispatcher.subscribe(ev)
            println("Subscribed to SR event subject: '$ev'")
        }
        val clockSubject = model.clock?.takeIf { it.isNotBlank() }
        if (clockSubject != null) {
            dispatcher.subscribe(clockSubject)
            println("Subscribed to clock subject: '$clockSubject'")
        } else {
            scheduler = Executors.newSingleThreadScheduledExecutor()
            scheduler!!.scheduleAtFixedRate({
                try { onTick() } catch (ex: Exception) { ex.printStackTrace() }
            }, 0, 100L, TimeUnit.MILLISECONDS)
        }
        running = true
        thread(start = true, isDaemon = false) { while (running) Thread.sleep(1000) }
    }

    private fun handleIncomingSrEvent(eventName: String) {
        println("[EVENT:SR] Received event '$eventName' at ${Instant.now()}")
        val evt = model.de.events.find { it.name == eventName }
        if (evt != null) {executeEventEffects(evt, "SR")}
        else{onTick()}
    }

    private fun onTick() {
        println("[TICK] ${Instant.now()}")
        val tickOnly = model.de.events.filter { it.trigger == null && it.name !in model.all_sr_events }
        tickOnly.forEach { executeEventEffects(it, "TICK") }
        val condEvents = model.de.events.filter { it.trigger is Trigger.VarCondition }
        for (evt in condEvents) {
            val trig = evt.trigger as Trigger.VarCondition
            val cur = synchronized(stateLock) { state[trig.variable] ?: 0.0 }
            val satisfied = when (trig.op) {
                "<" -> cur < trig.value
                "<=" -> cur <= trig.value
                ">" -> cur > trig.value
                ">=" -> cur >= trig.value
                "==" -> cur == trig.value
                "!=" -> cur != trig.value
                else -> false
            }
            if (satisfied) executeEventEffects(evt, "COND")
        }
    }

    private fun evalExpr(expr: rocketflow.model.Expr, state: Map<String, Double>): Double {
        return when (expr) {
            is rocketflow.model.Expr.Const -> expr.value
            is rocketflow.model.Expr.Variable -> state[expr.name] ?: 0.0
            is rocketflow.model.Expr.UnaryOp -> {
                val arg = evalExpr(expr.arg, state)
                when (expr.op) {
                    "+" -> +arg
                    "-" -> -arg
                    "sin" -> kotlin.math.sin(arg)
                    "cos" -> kotlin.math.cos(arg)
                    "abs" -> kotlin.math.abs(arg)
                    else -> error("Unknown unary op '${expr.op}'")
                }
            }
            is rocketflow.model.Expr.BinaryOp -> {
                val left = evalExpr(expr.left, state)
                val right = evalExpr(expr.right, state)
                when (expr.op) {
                    "+" -> left + right
                    "-" -> left - right
                    "*" -> left * right
                    "/" -> left / (right + 0.01)
                    else -> error("Unknown binary op '${expr.op}'")
                }
            }
        }
    }


    private fun executeEventEffects(evt: DEEvent, source: String) {
        println(" → Executing event '${evt.name}' (source=$source), effects=${evt.effects.size}")
        for ((idx, eff) in evt.effects.withIndex()) {
            when (eff) {
                is Effect.SetEnginePower -> {
                    val subj = "engine.${eff.engine}"
                    val payload = """{"cmd":"set_power","engine":"${eff.engine}","power":${eff.power}}"""
                    nc.publish(subj, payload.toByteArray())
                    println("    [Effect ${idx+1}] set_engine_power -> ${eff.engine} = ${eff.power}")
                }

                is Effect.UpdateVariable -> {
                    val deltaValue = evalExpr(eff.delta, state)
                    val old = synchronized(stateLock) { state.getOrDefault(eff.variable, 0.0) }
                    synchronized(stateLock) { state[eff.variable] = deltaValue }
                    println("    [Effect ${idx+1}] update_variable -> ${eff.variable}: $old -> ${deltaValue}")
                }

                is Effect.AddExternal -> {
                    val deltaValue = eff.delta?.let { evalExpr(it, state) } ?: 0.0
                    val old = synchronized(stateLock) { state.getOrDefault(eff.target, 0.0) }
                    synchronized(stateLock) { state[eff.target] = old + deltaValue }
                    println("    [Effect ${idx+1}] add_external -> ${eff.target}: $old -> ${old + deltaValue}")
                }

                else -> {
                    println("    [Effect ${idx+1}] Unknown effect type: $eff (ignored)")
                }
            }
        }
    }
    fun setInput(name: String, value: Double) { synchronized(stateLock) { state[name] = value } }
    fun getVariable(name: String) = synchronized(stateLock) { state[name] ?: 0.0 }

    fun stop() {
        println("Stopping DE Interpreter...")
        running = false
        scheduler?.shutdownNow()
        try { dispatcher.unsubscribe("*") } catch (_: Exception) {}
        try { nc.close() } catch (_: Exception) {}
        println("DE Interpreter stopped.")
    }
}
