package rocketflow

import rocketflow.runtime.DeInterpreter

fun main() {
    println("Starting RocketFlow DE interpreter...")

    val interpreter = DeInterpreter("resources/compiled.json")
    interpreter.load()
    interpreter.run()
}
