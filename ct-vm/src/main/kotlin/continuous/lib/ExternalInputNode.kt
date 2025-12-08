package continuous.lib
import continuous.CtNode
import io.nats.client.Dispatcher
import proto.Vector

class ExternalInputNode(
    override val id: String,
    override var isExternal: Boolean
) : CtNode {

    var value = Vector(x = 0.0, y = 0.0, rotate = 0.0)   // сюда NATS пишет внешние данные
    var topic: String = ""
    var component: String = ""
    lateinit var dispatcher: Dispatcher

    override val inputs = mutableMapOf<String, Double>()
    override val outputs = mutableMapOf<String, Double>()
    override val state = mutableMapOf<String, Double>()

    override fun derivatives() = emptyMap<String, Double>()

    override fun integrate(dt: Double) { }

    override fun updateOutputs() {
        if(component == "x") {
            outputs["out"] = value.x
        } else if (component == "y") {
            outputs["out"] = value.y
        } else if (component == "rotate"){
            outputs["out"] = value.rotate
        } else {
            outputs["out"] = 0.0
        }
    }
}
