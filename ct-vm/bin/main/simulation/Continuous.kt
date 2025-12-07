package simulation

interface ContinuousModel {
    fun advanceTime(dt: Double)
}