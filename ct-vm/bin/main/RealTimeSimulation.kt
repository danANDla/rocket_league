import org.knowm.xchart.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.BorderLayout
import javax.swing.SwingUtilities
import javax.swing.JFrame
import kotlin.math.E

fun runRealTimeSimulation() {
    val maxPoints = 400

    val gen = simulation.Generator(
        D = 0.0,   // при старте газ не нажат
        V = 0.0,
        T = 1.0 - 1.0 / E
    )

    val dt = 0.01
    val scheduler = simulation.Scheduler(gen, dt)

    val times = mutableListOf<Double>(0.0)
    val Ds = mutableListOf<Double>(gen.D)
    val Vs = mutableListOf<Double>(gen.V)

    val chart = XYChartBuilder()
        .width(800)
        .height(600)
        .title("Real-time Continuous Model")
        .xAxisTitle("time (s)")
        .yAxisTitle("Value")
        .build()

    chart.addSeries("D(t)", times, Ds)
    chart.addSeries("V(t)", times, Vs)

    val swingWrapper = SwingWrapper(chart)
    val frame = swingWrapper.displayChart()
    frame.isFocusable = true
    frame.requestFocus()

    // обработка клавиш
    frame.addKeyListener(object : KeyAdapter() {
        override fun keyPressed(e: KeyEvent) {
            if (e.keyCode == KeyEvent.VK_SPACE) {
                gen.D = 100.0
            }
        }

        override fun keyReleased(e: KeyEvent) {
            if (e.keyCode == KeyEvent.VK_SPACE) {
                gen.D = 0.0
            }
        }
    })

    // real-time loop (работает в отдельном потоке)
    Thread {
        while (true) {
            scheduler.runUntil(scheduler.currentTime + dt)

            times.add(scheduler.currentTime)
            Ds.add(gen.D)
            Vs.add(gen.V)

            while (times.size > maxPoints) {
                times.removeAt(0)
                Ds.removeAt(0)
                Vs.removeAt(0)
            }


            // Обновить график каждые ~20 мс
            if (times.size % 2 == 0) {
                chart.updateXYSeries("D(t)", times, Ds, null)
                chart.updateXYSeries("V(t)", times, Vs, null)
                swingWrapper.repaintChart()
            }

            Thread.sleep((dt * 1000).toLong()) // real time pace
        }
    }.start()
}

fun rocketPanelSimulation() {
    val maxPoints = 400
    val gen = simulation.Generator(
        D = 0.0,
        V = 0.0,
        T = 1 - 1/E
    )

    val dt = 0.01
    val scheduler = simulation.Scheduler(gen, dt)

    // === UI ===
    val panel = RocketPanel(
        getD = { gen.D },
        getV = { gen.V }
    )

    val frame = JFrame("Rocket Simulation")
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.setSize(800, 600)
    frame.add(panel, BorderLayout.CENTER)
    frame.isVisible = true
    frame.isFocusable = true
    frame.requestFocusInWindow()

    // Управление "газом"
    frame.addKeyListener(object : KeyAdapter() {
        override fun keyPressed(e: KeyEvent) {
            if (e.keyCode == KeyEvent.VK_SPACE) gen.D = 100.0
        }

        override fun keyReleased(e: KeyEvent) {
            if (e.keyCode == KeyEvent.VK_SPACE) gen.D = 0.0
        }
    })

    val times = mutableListOf<Double>(0.0)
    val Ds = mutableListOf<Double>(gen.D)
    val Vs = mutableListOf<Double>(gen.V)

    val chart = XYChartBuilder()
        .width(500)
        .height(200)
        .title("Real-time Continuous Model")
        .xAxisTitle("time (s)")
        .yAxisTitle("Value")
        .build()
    chart.addSeries("D(t)", times, Ds)
    chart.addSeries("V(t)", times, Vs)
    val chartPanel = XChartPanel(chart);
    frame.add(chartPanel, BorderLayout.SOUTH)

    // === Реальное время ===
    Thread {
        while (true) {
            scheduler.runUntil(scheduler.currentTime + dt)

            times.add(scheduler.currentTime)
            Ds.add(gen.D)
            Vs.add(gen.V)
            while (times.size > maxPoints) {
                times.removeAt(0)
                Ds.removeAt(0)
                Vs.removeAt(0)
            }
            // Обновить график каждые ~20 мс
            if (times.size % 2 == 0) {
                chart.updateXYSeries("D(t)", times, Ds, null)
                chart.updateXYSeries("V(t)", times, Vs, null)
                chartPanel.revalidate()
                chartPanel.repaint()
            }
            Thread.sleep((dt * 1000).toLong()) // real time pace
        }
    }.start()

}