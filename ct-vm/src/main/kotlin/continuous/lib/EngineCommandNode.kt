package continuous.lib
import continuous.CtNode
import io.nats.client.Dispatcher
import proto.MsgCommand

class EngineCommandNode(
    override val id: String,
    override var isExternal: Boolean
) : CtNode {

    var value = MsgCommand(cmd = "", engine = "", power = 0.0)  // сюда NATS пишет внешние данные
    var topic: String = ""
    var component: String = ""
    lateinit var dispatcher: Dispatcher

    override val inputs = mutableMapOf<String, Double>()
    override val outputs = mutableMapOf<String, Double>()
    override val state = mutableMapOf<String, Double>()

    override fun derivatives() = emptyMap<String, Double>()

    override fun integrate(dt: Double) { }

    override fun updateOutputs() {
        if(component == "power") {
            outputs["out"] = value.power
        } else {
            outputs["out"] = 0.0
        }
    }
}
