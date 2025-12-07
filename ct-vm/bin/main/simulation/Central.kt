package simulation

class Scheduler(
    val model: ContinuousModel,
    val dt: Double
) {
    var currentTime = 0.0

    fun runUntil(endTime: Double) {
        while (currentTime < endTime) {
            model.advanceTime(dt)
            currentTime += dt
        }
    }
}
