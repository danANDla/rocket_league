package rocketflow

import java.io.File

class DeParser {

    fun parseFile(f: File): Pair<DEModel, String?> {
        val variables = linkedMapOf<String, Double>()
        val events = mutableListOf<DEEvent>()
        val engines = linkedSetOf<String>()
        var clock: String? = null

        val lines = f.readLines()
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            i++
            if (line.isEmpty() || line.startsWith("#")) continue

            when {
                line.startsWith("clock") -> clock = line.removePrefix("clock").trim()
                line.startsWith("var ") -> {
                    val parts = line.removePrefix("var").split("=").map { it.trim() }
                    if (parts.size == 2) {
                        variables[parts[0]] = parts[1].toDoubleOrNull() ?: 0.0
                    }
                }
                line.startsWith("event ") -> {
                    val name = line.removePrefix("event").trim().trimEnd(':')
                    val effects = mutableListOf<Effect>()
                    var trigger: Trigger? = null

                    while (i < lines.size) {
                        val nxt = lines[i].trim()
                        if (nxt.isEmpty() || nxt.startsWith("#")) { i++; continue }
                        if (nxt.startsWith("event") || nxt.startsWith("var") || nxt.startsWith("clock") || nxt.startsWith("on")) break

                        when {
                            nxt.startsWith("trigger ") -> {
                                val exprStr = nxt.removePrefix("trigger").trim()
                                trigger = Trigger.ExprTrigger(ExprParser.parse(exprStr))
                            }
                            nxt.startsWith("action update ") -> {
                                val rest = nxt.removePrefix("action update").trim()
                                val spaceIndex = rest.indexOf(' ')
                                if (spaceIndex > 0) {
                                    val varName = rest.substring(0, spaceIndex)
                                    val exprStr = rest.substring(spaceIndex + 1)
                                    effects.add(Effect.UpdateVariable(varName, ExprParser.parse(exprStr)))
                                }
                            }
                            nxt.startsWith("add ") -> {
                                val rest = nxt.removePrefix("add").trim()
                                val spaceIndex = rest.indexOf(' ')
                                if (spaceIndex > 0) {
                                    val target = rest.substring(0, spaceIndex)
                                    val exprStr = rest.substring(spaceIndex + 1)
                                    effects.add(Effect.AddExternal(target, ExprParser.parse(exprStr)))
                                } else {
                                    effects.add(Effect.AddExternal(rest, null))
                                }
                            }
                        }
                        i++
                    }

                    events.add(DEEvent(name, trigger, effects))
                }
                line.startsWith("on ") -> {
                    val rest = line.removePrefix("on").trim()
                    val parts = rest.split("->").map { it.trim() }
                    if (parts.size == 2) {
                        val evName = parts[0]
                        val action = parts[1]
                        if (action.startsWith("add ")) {
                            val restAction = action.removePrefix("add").trim()
                            val spaceIndex = restAction.indexOf(' ')
                            if (spaceIndex > 0) {
                                val target = restAction.substring(0, spaceIndex)
                                val exprStr = restAction.substring(spaceIndex + 1)
                                events.add(DEEvent(evName, null, listOf(Effect.AddExternal(target, ExprParser.parse(exprStr)))))
                            } else {
                                events.add(DEEvent(evName, null, listOf(Effect.AddExternal(restAction, null))))
                            }
                        } else {
                            events.add(DEEvent(evName, null, emptyList()))
                        }
                    }
                }
            }
        }

        return DEModel(variables.toMap(), events, engines.toList()) to clock
    }
}
