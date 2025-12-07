import java.awt.*
import javax.swing.*
import kotlin.random.Random

class RocketPanel(
    private val getD: () -> Double,
    private val getV: () -> Double
) : JPanel() {

    private val particles = mutableListOf<Particle>()

    init {
        background = Color.BLACK
        // Таймер Swing каждые 16 ms (~60 FPS) обновляет анимацию
        Timer(16) {
            updateParticles()
            repaint()
        }.start()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        drawRocket(g2)
        drawParticles(g2)
    }

    private fun drawRocket(g: Graphics2D) {
        val cx = width / 2
        val cy = height / 2

        val rocketHeight = 80
        val rocketWidth = 30

        // РАКЕТА — конус
        val xPoints = intArrayOf(cx, cx - rocketWidth / 2, cx + rocketWidth / 2)
        val yPoints = intArrayOf(cy - rocketHeight / 2, cy + rocketHeight / 2, cy + rocketHeight / 2)

        g.color = Color.LIGHT_GRAY
        g.fillPolygon(xPoints, yPoints, 3)

        // ПЛАМЯ — если D == 100
        if (getD() > 50) {
            g.color = Color.ORANGE
            g.fillOval(cx - 10, cy + rocketHeight / 2, 20, 30)
        }
    }

    private fun updateParticles() {
        if(width <= 0 || height <= 0) return

        val V = getV()

        // Добавляем новые частицы пропорционально скорости
        repeat((V / 5).toInt().coerceAtLeast(1)) {
            particles.add(
                Particle(
                    x = Random.nextInt(width),
                    y = 0.0,
                    speed = V / 3 + Random.nextDouble(1.0, 3.0)
                )
            )
        }

        // Обновляем позиции
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.y += p.speed
            p.speed = V / 3 + Random.nextDouble(1.0, 3.0)
            if (p.y > height.toDouble()) iterator.remove()
        }
    }

    private fun drawParticles(g: Graphics2D) {
        g.color = Color.WHITE
        for (p in particles) {
            g.fillRect(p.x.toInt(), p.y.toInt(), 2, 4) // черточки
        }
    }

    data class Particle(var x: Int, var y: Double, var speed: Double)
}
