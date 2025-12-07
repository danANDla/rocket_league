import org.knowm.xchart.SwingWrapper
import org.knowm.xchart.XYChartBuilder

fun plotSimulation(times: List<Double>, D: List<Double>, V: List<Double>) {
    val chart = XYChartBuilder()
        .width(800)
        .height(600)
        .title("Continuous-time model: D and V")
        .xAxisTitle("time (s)")
        .yAxisTitle("Value")
        .build()

    chart.addSeries("D(t)", times, D)
    chart.addSeries("V(t)", times, V)

    SwingWrapper(chart).displayChart()
}