package continuous

interface CtNode {
    val id: String

    // Входы: имя → значение
    val inputs: MutableMap<String, Double>

    // Выходы: имя → значение
    val outputs: MutableMap<String, Double>

    // Внутреннее состояние (динамические переменные x(t))
    val state: MutableMap<String, Double>

    /**
     * Рассчитать производные dx/dt на основе текущих входов/состояния
     */
    fun derivatives(): Map<String, Double>

    /**
     * Выполнить интеграцию состояния: x += dx/dt * dt
     */
    fun integrate(dt: Double) {
        val d = derivatives()
        for ((k, dv) in d) {
            state[k] = (state[k] ?: 0.0) + dv * dt
        }
    }

    /**
     * Обновить выходы узла после шага (обычно зависит от state)
     */
    fun updateOutputs()
}