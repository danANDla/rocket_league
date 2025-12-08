package proto

import kotlinx.serialization.Serializable

@Serializable
data class TimeTick(val t: Double, val dt: Double)

@Serializable
data class CtState(val value: Double)

@Serializable
data class CtStateSnapshot(val nodes: Map<String, Map<String, Double>>)

@Serializable
data class CtExternalUpdate(val externalValueName: String, val value: Double)

@Serializable
data class SrState(val outputs: Map<String, Int>)

@Serializable
data class DeEvent(val time: Double, val payload: String)

@Serializable
data class Vector(
    val x: Double,
    val y: Double,
    val rotate: Double
)

@Serializable
data class MsgCommand(
    val cmd: String,
    val engine: String,
    val power: Double
)

@Serializable
data class SimulationFootprint(
   val coordinates: Vector,
   val velocity: Vector,
   val acceleration: Vector,
   val time: Double
)