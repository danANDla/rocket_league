package simulation

class Generator(
    var D: Double,     // throttle input
    var V: Double,     // power output (state)
    val T: Double      // time constant
) : ContinuousModel {

    override fun advanceTime(dt: Double) {
        val dVdt = (D - V) / T
        V += dVdt * dt
    }
}