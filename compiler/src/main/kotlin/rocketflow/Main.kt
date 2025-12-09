package rocketflow

import kotlinx.serialization.json.Json
import java.io.File

fun main() {
    println("RocketFlow compiler — building extended JSON model")

    val examplesDir = File("examples")
    if (!examplesDir.exists() || !examplesDir.isDirectory) {
        error("examples/ folder not found")
    }

    // ------------------- Сбор файлов -------------------
    val srFiles = examplesDir.listFiles { _, name -> name.endsWith(".sr.dsl") }?.toList() ?: emptyList()
    val deFiles = examplesDir.listFiles { _, name -> name.endsWith(".de.dsl") }?.toList() ?: emptyList()
    val formulaFiles = examplesDir.listFiles { _, name -> name.endsWith(".fm.dsl") }?.toList() ?: emptyList()

    if (srFiles.isEmpty()) println("Warning: no SR files found")
    if (deFiles.isEmpty()) println("Warning: no DE files found")
    if (formulaFiles.isEmpty()) println("Warning: no formula files found")

    val srParser = SrParser()
    val deParser = DeParser()

    // ------------------- Парсинг SR -------------------
    val allSrRules = mutableListOf<SrRule>()
    for (f in srFiles) {
        println("Parsing SR file: ${f.path}")
        allSrRules += srParser.parseFile(f)
    }

    val continuousInputs = srParser.findContinuousInputs(allSrRules)

    // ------------------- Сбор всех SR-триггеров -------------------
    val allSrEvents = allSrRules.map { it.trigger }.distinct()

    // ------------------- Парсинг DE -------------------
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

    // ------------------- Сбор полного DE-моделя -------------------
    val fullDE = DEModel(
        variables = mergedVars.toMap(),
        events = mergedEvents,
        engines = mergedEngines.toList()
    )

    // ------------------- Генерация compiled.json -------------------
    val fullModel = ExtendedFullModel(
        all_sr_events = allSrEvents,
        continuous_inputs = continuousInputs,
        clock = clock,
        sr_rules = allSrRules,
        de = fullDE
    )

    val json = Json { prettyPrint = true }
    File("compiled.json").writeText(json.encodeToString(ExtendedFullModel.serializer(), fullModel))
    println("Wrote compiled.json")

    // ------------------- Генерация env.json -------------------
    formulaFiles.forEach { f ->
        println("Parsing formula file for env.json: ${f.path}")
        val env = generateEnvJson(
            formulaFile = f,
            continuousInputs = continuousInputs,
            deEvents = fullModel.de.events
        )
        val json = Json { prettyPrint = true }
        File("env.json").writeText(json.encodeToString(EnvJson.serializer(), env))
        println("env.json successfully generated")
    }
}
