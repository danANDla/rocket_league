package rocketflow

import kotlinx.serialization.json.Json
import java.io.File

/**
 * RocketFlow compiler — extended version
 * Добавляет:
 * - сбор всех SR событий в поле all_sr_events
 * - минимальная валидация, предполагается корректный ввод
 */

fun main() {
    println("RocketFlow compiler — building extended JSON model")

    val examplesDir = File("examples")
    if (!examplesDir.exists() || !examplesDir.isDirectory) {
        error("examples/ folder not found")
    }

    val srFiles = examplesDir.listFiles { _, name -> name.endsWith(".sr.dsl") }?.toList() ?: emptyList()
    val deFiles = examplesDir.listFiles { _, name -> name.endsWith(".de.dsl") }?.toList() ?: emptyList()

    if (srFiles.isEmpty()) println("Warning: no SR files found")
    if (deFiles.isEmpty()) println("Warning: no DE files found")

    val srParser = SrParser()
    val deParser = DeParser()

    // --- Parse SR files ---
    val allSrRules = mutableListOf<SrRule>()
    for (f in srFiles) {
        println("Parsing SR file: ${f.path}")
        allSrRules += srParser.parseFile(f)
    }

    val continuousInputs = srParser.findContinuousInputs(allSrRules)

    // --- NEW: collect all SR-triggered events ---
    val allSrEvents = allSrRules.map { it.trigger }.distinct()

    // --- Merge DE models ---
    val mergedVars = linkedMapOf<String, Double>()
    val mergedEvents = mutableListOf<DEEvent>()
    val mergedEngines = linkedSetOf<String>()
    var clock: String? = null

    for (f in deFiles) {
        println("Parsing DE file: ${f.path}")
        val (deModel, c) = deParser.parseFile(f)

        if (c != null && clock == null) clock = c

        for ((k, v) in deModel.variables) {
            if (!mergedVars.containsKey(k)) mergedVars[k] = v
        }

        mergedEvents += deModel.events
        mergedEngines += deModel.engines
    }

    // --- prepare extended output ---
    val full = ExtendedFullModel(
        all_sr_events = allSrEvents,
        continuous_inputs = continuousInputs,
        clock = clock,
        sr_rules = allSrRules,
        de = DEModel(
            variables = mergedVars.toMap(),
            events = mergedEvents,
            engines = mergedEngines.toList()
        )
    )

    val json = Json { prettyPrint = true }
    val out = json.encodeToString(ExtendedFullModel.serializer(), full)

    File("compiled.json").writeText(out)

    println(
        "Wrote compiled.json " +
        "(events_from_SR=${full.all_sr_events.size}, " +
        "continuous_inputs=${full.continuous_inputs.size}, " +
        "sr_rules=${full.sr_rules.size}, " +
        "de.events=${full.de.events.size})"
    )
}
