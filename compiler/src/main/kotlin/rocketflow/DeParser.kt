package rocketflow

import java.io.File

/**
 * Упрощённый парсер DE DSL.
 *
 * Поддерживаем синтаксис:
 *
 * clock 10ms
 *
 * var <name> = <number>
 *
 * event <name>:
 *   trigger <var> <op> <value>
 *   set <engine> power <value>
 *   action update <var> <delta>
 *
 * Также поддерживается короткий синтаксис:
 * on <EVENT> -> <actionName>
 *   где <actionName> можно трактовать как set <engine> power <val> если нужно.
 *
 * Мы считаем, что input корректен.
 */

class DeParser {
    fun parseFile(f: File): Pair<DEModel, String?> {
        val variables = linkedMapOf<String, Double>()
        val events = mutableListOf<DEEvent>()
        val engines = linkedSetOf<String>()
        var clock: String? = null

        val lines = f.readLines()
        var i = 0
        while (i < lines.size) {
            var line = lines[i].trim()
            i++
            if (line.isEmpty() || line.startsWith("#")) continue

            if (line.startsWith("clock ")) {
                clock = line.removePrefix("clock").trim()
                continue
            }
            if (line.startsWith("var ")) {
                // var name = 100.0
                val rest = line.removePrefix("var").trim()
                val parts = rest.split("=").map { it.trim() }
                if (parts.size == 2) {
                    val name = parts[0]
                    val v = parts[1].toDoubleOrNull() ?: 0.0
                    variables[name] = v
                }
                continue
            }
            if (line.startsWith("event ")) {
                // event name:  (or without colon)
                val name = line.removePrefix("event").trim().trimEnd(':').trim()
                val effects = mutableListOf<Effect>()
                var trigger: Trigger? = null
                // read following indented lines (or until next event/clock/var)
                while (i < lines.size) {
                    val nxt = lines[i].trim()
                    if (nxt.isEmpty() || nxt.startsWith("#")) { i++; continue }
                    if (nxt.startsWith("event ") || nxt.startsWith("var ") || nxt.startsWith("clock ") || nxt.startsWith("on ")) break
                    if (nxt.startsWith("trigger ")) {
                        val body = nxt.removePrefix("trigger").trim()
                        // expecting: <var> <op> <value>
                        val tokens = body.split(Regex("\\s+"))
                        if (tokens.size >= 3) {
                            val varName = tokens[0]
                            val op = tokens[1]
                            val value = tokens[2].toDoubleOrNull() ?: 0.0
                            trigger = Trigger.VarCondition(variable = varName, op = op, value = value)
                        }
                    } else if (nxt.startsWith("set ")) {
                        // set engine1 power 0.5
                        val body = nxt.removePrefix("set").trim()
                        val parts = body.split(Regex("\\s+"))
                        if (parts.size >= 3 && parts[1] == "power") {
                            val engine = parts[0]
                            val power = parts[2].toDoubleOrNull() ?: 0.0
                            engines.add(engine)
                            effects.add(Effect.SetEnginePower(engine = engine, power = power))
                        }
                    } else if (nxt.startsWith("action update ")) {
                        val body = nxt.removePrefix("action update").trim()
                        val toks = body.split(Regex("\\s+"))
                        if (toks.size >= 2) {
                            val varName = toks[0]
                            val delta = toks[1].toDoubleOrNull() ?: 0.0
                            effects.add(Effect.UpdateVariable(variable = varName, delta = delta))
                        }
                    } else if (nxt.startsWith("action ")) {
                        // action <something> — try simple patterns: update var -1
                        val body = nxt.removePrefix("action").trim()
                        if (body.startsWith("update ")) {
                            val b = body.removePrefix("update").trim()
                            val toks = b.split(Regex("\\s+"))
                            if (toks.size >= 2) {
                                val varName = toks[0]
                                val delta = toks[1].toDoubleOrNull() ?: 0.0
                                effects.add(Effect.UpdateVariable(variable = varName, delta = delta))
                            }
                        }
                    }
                    i++
                }
                events.add(DEEvent(name = name, trigger = trigger, effects = effects))
                continue
            }
            if (line.startsWith("on ")) {
                // on EVENT -> action  (simple shorthand)
                val rest = line.removePrefix("on").trim()
                val parts = rest.split("->").map { it.trim() }
                if (parts.size == 2) {
                    val evName = parts[0]
                    val action = parts[1] // e.g., thrust_down or set engine1 power 0.5
                    // try parse "set <engine> power <value>"
                    if (action.startsWith("set ")) {
                        val b = action.removePrefix("set").trim()
                        val toks = b.split(Regex("\\s+"))
                        if (toks.size >= 3 && toks[1] == "power") {
                            val engine = toks[0]
                            val power = toks[2].toDoubleOrNull() ?: 0.0
                            engines.add(engine)
                            events.add(DEEvent(name = evName, trigger = null, effects = listOf(Effect.SetEnginePower(engine, power))))
                        } else {
                            // otherwise create a placeholder effect-less event
                            events.add(DEEvent(name = evName, trigger = null, effects = emptyList()))
                        }
                    } else {
                        // placeholder event with no effects
                        events.add(DEEvent(name = evName, trigger = null, effects = emptyList()))
                    }
                }
            }
        } // while
        val model = DEModel(variables = variables.toMap(), events = events, engines = engines.toList())
        return Pair(model, clock)
    }
}
